package com.saurabhsandav.fyers_api

import com.saurabhsandav.fyers_api.model.CandleResolution
import com.saurabhsandav.fyers_api.model.DateFormat
import com.saurabhsandav.fyers_api.model.request.AuthValidationRequest
import com.saurabhsandav.fyers_api.model.request.RefreshValidationRequest
import com.saurabhsandav.fyers_api.model.response.*
import com.slack.eithernet.ApiResult
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import java.io.IOException

public class FyersApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val rateLimiter = FyersRateLimiter()

    public fun getLoginURL(redirectUrl: String): String {
        return URLBuilder("https://api-t1.fyers.in/api/v3/generate-authcode").apply {
            encodedParameters.apply {
                append("client_id", BuildKonfig.FYERS_APP_ID)
                append("redirect_uri", redirectUrl)
                append("response_type", "code")
                append("state", "trading_companion")
            }
        }.buildString()
    }

    public suspend fun validateLogin(
        redirectUrl: String,
    ): ApiResult<AuthValidationResult, FyersError> {

        // Rate-limit
        rateLimiter.limit()

        val requestBody = AuthValidationRequest(
            grantType = "authorization_code",
            appIdHash = "${BuildKonfig.FYERS_APP_ID}:${BuildKonfig.FYERS_SECRET}".sha256(),
            code = Url(redirectUrl).parameters["auth_code"] ?: error("Invalid redirectionUrl"),
        )

        return client.runRequestAndGetApiResult {
            method = HttpMethod.Post
            url("https://api-t1.fyers.in/api/v3/validate-authcode")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
    }

    public suspend fun refreshLogin(
        refreshToken: String,
        pin: String,
    ): ApiResult<RefreshValidationResult, FyersError> {

        // Rate-limit
        rateLimiter.limit()

        val requestBody = RefreshValidationRequest(
            grantType = "refresh_token",
            appIdHash = "${BuildKonfig.FYERS_APP_ID}:${BuildKonfig.FYERS_SECRET}".sha256(),
            refreshToken = refreshToken,
            pin = pin,
        )

        return client.runRequestAndGetApiResult {
            method = HttpMethod.Post
            url("https://api-t1.fyers.in/api/v3/validate-refresh-token")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
    }

    public suspend fun getProfile(
        accessToken: String,
    ): ApiResult<ProfileResult, FyersError> {

        // Rate-limit
        rateLimiter.limit()

        return client.runRequestAndGetApiResult {
            method = HttpMethod.Get
            url("https://api-t1.fyers.in/api/v3/profile")
            header("Authorization", "${BuildKonfig.FYERS_APP_ID}:$accessToken")
        }
    }

    public suspend fun getHistoricalCandles(
        accessToken: String,
        symbol: String,
        resolution: CandleResolution,
        dateFormat: DateFormat,
        rangeFrom: String,
        rangeTo: String,
    ): ApiResult<HistoricalCandlesResult, FyersError> {

        // Rate-limit
        rateLimiter.limit()

        return client.runRequestAndGetApiResult {
            method = HttpMethod.Get
            url("https://api-t1.fyers.in/data/history")
            header("Authorization", "${BuildKonfig.FYERS_APP_ID}:$accessToken")
            parameter("symbol", symbol)
            parameter("resolution", resolution.strValue)
            parameter("date_format", dateFormat.intValue)
            parameter("range_from", rangeFrom)
            parameter("range_to", rangeTo)
            parameter("cont_flag", "")
        }
    }

    public suspend fun getQuotes(
        accessToken: String,
        symbols: List<String>,
    ): ApiResult<Quotes, FyersError> {

        // Rate-limit
        rateLimiter.limit()

        return client.runRequestAndGetApiResult {
            method = HttpMethod.Get
            url("https://api-t1.fyers.in/data/quotes")
            header("Authorization", "${BuildKonfig.FYERS_APP_ID}:$accessToken")
            parameter("symbols", symbols.joinToString(","))
        }
    }

    private suspend fun String.sha256(): String {
        return CryptographyProvider.Default.get(SHA256).hasher().hash(encodeToByteArray()).toHexString()
    }

    private suspend inline fun <reified T : Any> HttpClient.runRequestAndGetApiResult(
        block: HttpRequestBuilder.() -> Unit,
    ): ApiResult<T, FyersError> = try {

        val response = request(block)
        val responseCode = response.status.value
        val fyersResponse = response.body<FyersResponse<T>>()

        when {
            responseCode in 400..599 -> ApiResult.httpFailure(responseCode, fyersResponse.getError())
            response.status.isSuccess() && fyersResponse.code != 200 -> ApiResult.apiFailure(fyersResponse.getError())
            response.status.isSuccess() -> ApiResult.success(fyersResponse.result!!)
            else -> ApiResult.apiFailure()
        }
    } catch (e: IOException) {
        ApiResult.networkFailure(e)
    } catch (e: Exception) {
        ensureActive()
        ApiResult.unknownFailure(e)
    }
}

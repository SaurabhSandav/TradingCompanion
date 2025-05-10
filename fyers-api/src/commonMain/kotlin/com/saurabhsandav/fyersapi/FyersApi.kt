package com.saurabhsandav.fyersapi

import app.softwork.serialization.csv.CSVFormat
import com.saurabhsandav.fyersapi.model.CandleResolution
import com.saurabhsandav.fyersapi.model.DateFormat
import com.saurabhsandav.fyersapi.model.request.AuthValidationRequest
import com.saurabhsandav.fyersapi.model.request.RefreshValidationRequest
import com.saurabhsandav.fyersapi.model.response.AuthValidationResult
import com.saurabhsandav.fyersapi.model.response.FyersError
import com.saurabhsandav.fyersapi.model.response.FyersResponse
import com.saurabhsandav.fyersapi.model.response.HistoricalCandlesResult
import com.saurabhsandav.fyersapi.model.response.ProfileResult
import com.saurabhsandav.fyersapi.model.response.Quotes
import com.saurabhsandav.fyersapi.model.response.RefreshValidationResult
import com.saurabhsandav.fyersapi.model.response.Symbol
import com.saurabhsandav.fyersapi.model.response.getError
import com.slack.eithernet.ApiResult
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.time.Duration.Companion.hours

public class FyersApi(
    private val cachePath: Path,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val csvFormat = CSVFormat {
        includeHeader = false
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

    public suspend fun validateLogin(redirectUrl: String): ApiResult<AuthValidationResult, FyersError> {

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

    public suspend fun getProfile(accessToken: String): ApiResult<ProfileResult, FyersError> {

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

    public suspend fun getNseCapitalMarketSymbols(): List<Symbol> = getSymbols(
        cacheFileName = "NSE_CM",
        url = "https://public.fyers.in/sym_details/NSE_CM.csv",
    )

    public suspend fun getNseEquityDerivativeSymbols(): List<Symbol> = getSymbols(
        cacheFileName = "NSE_FO",
        url = "https://public.fyers.in/sym_details/NSE_FO.csv",
    )

    public suspend fun getNseCurrencyDerivativeSymbols(): List<Symbol> = getSymbols(
        cacheFileName = "NSE_CD",
        url = "https://public.fyers.in/sym_details/NSE_CD.csv",
    )

    public suspend fun getBseCapitalMarketSymbols(): List<Symbol> = getSymbols(
        cacheFileName = "BSE_CM",
        url = "https://public.fyers.in/sym_details/BSE_CM.csv",
    )

    public suspend fun getMcxCommoditySymbols(): List<Symbol> = getSymbols(
        cacheFileName = "MCX_COM",
        url = "https://public.fyers.in/sym_details/MCX_COM.csv",
    )

    private suspend fun getSymbols(
        cacheFileName: String,
        url: String,
    ): List<Symbol> = withContext(Dispatchers.IO) {

        val fyersDir = cachePath.resolve(FyersApiCachePath)
        val filePath = fyersDir.resolve(cacheFileName)

        // Cache file doesn't exist or cache file is older than 24 hours
        val shouldDownloadSymbols = filePath.notExists() ||
            run {
                val lastModifiedTime = filePath.getLastModifiedTime()
                (lastModifiedTime.toInstant().toKotlinInstant() + 24.hours) < Clock.System.now()
            }

        // Download symbol data file
        if (shouldDownloadSymbols) {

            // Create folders if they don't exist
            if (fyersDir.notExists()) fyersDir.createDirectories()

            // Download symbol data
            val symbolData = client.get(url).body<ByteArray>()

            // Write symbol data to file
            filePath.writeBytes(symbolData)
        }

        val symbolData = filePath.readText()
        val symbols = csvFormat.decodeFromString<List<Symbol>>(symbolData)

        return@withContext symbols
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

    private companion object {
        private const val FyersApiCachePath = "FyersApi"
    }
}

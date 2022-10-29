package fyers_api

import BuildKonfig
import com.github.michaelbull.result.get
import com.github.michaelbull.result.runCatching
import com.soywiz.krypto.sha256
import fyers_api.model.CandleResolution
import fyers_api.model.DateFormat
import fyers_api.model.request.AuthValidationRequest
import fyers_api.model.response.AuthValidationResult
import fyers_api.model.response.FyersResponse
import fyers_api.model.response.HistoricalCandlesResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FyersApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private val redirectURL = "http://localhost:8080"

    fun getLoginURL(): String {
        return URLBuilder("https://api.fyers.in/api/v2/generate-authcode").apply {
            encodedParameters.apply {
                append("client_id", BuildKonfig.FYERS_APP_ID)
                append("redirect_uri", redirectURL)
                append("response_type", "code")
                append("state", "trading_companion")
            }
        }.buildString()
    }

    suspend fun getAccessToken(redirectUrl: String): FyersResponse<AuthValidationResult> {

        val requestBody = AuthValidationRequest(
            code = Url(redirectUrl).parameters["auth_code"] ?: error("Invalid redirectionUrl"),
            appIdHash = "${BuildKonfig.FYERS_APP_ID}:${BuildKonfig.FYERS_SECRET}".toByteArray().sha256().toString(),
            grantType = "authorization_code",
        )

        val response = client.post("https://api.fyers.in/api/v2/validate-authcode") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        return response.decodeToFyersResponse()
    }

    suspend fun getHistoricalCandles(
        accessToken: String,
        symbol: String,
        resolution: CandleResolution,
        dateFormat: DateFormat,
        rangeFrom: String,
        rangeTo: String,
    ): FyersResponse<HistoricalCandlesResult> {

        val response = client.get("https://api.fyers.in/data-rest/v2/history/") {
            header("Authorization", "${BuildKonfig.FYERS_APP_ID}:$accessToken")
            parameter("symbol", symbol)
            parameter("resolution", resolution.strValue)
            parameter("date_format", dateFormat.intValue)
            parameter("range_from", rangeFrom)
            parameter("range_to", rangeTo)
            parameter("cont_flag", "")
        }

        return response.decodeToFyersResponse()
    }

    private suspend inline fun <reified T> HttpResponse.decodeToFyersResponse(): FyersResponse<T> {

        val jsonObject = json.parseToJsonElement(bodyAsText()).jsonObject

        return FyersResponse(
            s = jsonObject["s"]?.jsonPrimitive?.content,
            code = jsonObject["code"]?.jsonPrimitive?.intOrNull,
            message = jsonObject["message"]?.jsonPrimitive?.content,
            statusCode = status,
            result = runCatching<T> { body() }.get(),
        )
    }
}

package fyers_api

import BuildKonfig
import com.soywiz.krypto.sha256
import fyers_api.model.CandleResolution
import fyers_api.model.DateFormat
import fyers_api.model.request.AuthValidationRequest
import fyers_api.model.response.AuthValidationResult
import fyers_api.model.response.FyersResponse
import fyers_api.model.response.FyersResult
import fyers_api.model.response.HistoricalCandlesResult
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

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

        return response.decodeToFyersReponse()
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

        return response.decodeToFyersReponse()
    }

    private suspend inline fun <reified T : FyersResult> HttpResponse.decodeToFyersReponse(): FyersResponse<T> {

        val jsonObject = json.parseToJsonElement(bodyAsText()).jsonObject

        val successStr = jsonObject["s"]?.jsonPrimitive?.content ?: error("FyersResponse: No 's' field found")

        return when (successStr) {
            "ok" -> FyersResponse.Success(
                s = successStr,
                code = jsonObject["code"]?.jsonPrimitive?.intOrNull,
                message = jsonObject["message"]?.jsonPrimitive?.content,
                result = run {
                    val resultObject = JsonObject(jsonObject.filter { it.key !in listOf("s", "code", "message") })
                    json.decodeFromJsonElement<T>(resultObject)
                }
            )
            else -> FyersResponse.Failure(
                s = successStr,
                code = jsonObject["code"]?.jsonPrimitive?.intOrNull!!,
                message = jsonObject["message"]?.jsonPrimitive?.content!!,
            )
        }
    }
}

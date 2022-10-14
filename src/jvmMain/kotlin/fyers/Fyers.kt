package fyers

import BuildKonfig
import com.soywiz.krypto.sha256
import fyers.model.CandleResolution
import fyers.model.DateFormat
import fyers.model.request.AuthValidationRequest
import fyers.model.response.AuthValidationResponse
import fyers.model.response.HistoryResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class Fyers(private val client: HttpClient) {

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

    suspend fun getAccessToken(redirectUrl: String): String {

        val requestBody = AuthValidationRequest(
            code = Url(redirectUrl).parameters["auth_code"] ?: error("Invalid redirectionUrl"),
            appIdHash = "${BuildKonfig.FYERS_APP_ID}:${BuildKonfig.FYERS_SECRET}".toByteArray().sha256().toString(),
            grantType = "authorization_code",
        )

        val response = client.post("https://api.fyers.in/api/v2/validate-authcode") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }.body<AuthValidationResponse>()

        if (response.s != "ok")
            throw Exception(response.message)

        return response.accessToken!!
    }

    suspend fun getHistoricalCandles(
        accessToken: String,
        symbol: String,
        resolution: CandleResolution,
        dateFormat: DateFormat,
        rangeFrom: String,
        rangeTo: String,
    ): List<List<String>> {

        val response = client.get("https://api.fyers.in/data-rest/v2/history/") {
            header("Authorization", "${BuildKonfig.FYERS_APP_ID}:$accessToken")
            parameter("symbol", symbol)
            parameter("resolution", resolution.strValue)
            parameter("date_format", dateFormat.intValue)
            parameter("range_from", rangeFrom)
            parameter("range_to", rangeTo)
            parameter("cont_flag", "")
        }.body<HistoryResponse>()

        if (response.s != "ok")
            throw Exception("Request getHistoricalCandles failed!")

        return response.candles
    }
}

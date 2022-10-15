package fyers_api.model.response

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthValidationResponse(

    @SerialName("s")
    val s: String,

    @SerialName("code")
    val code: Int,

    @SerialName("message")
    val message: String,

    @SerialName("access_token")
    val accessToken: String?,
)

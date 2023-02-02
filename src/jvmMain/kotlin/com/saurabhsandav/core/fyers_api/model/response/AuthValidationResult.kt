package com.saurabhsandav.core.fyers_api.model.response

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthValidationResult(

    @SerialName("access_token")
    val accessToken: String,
)

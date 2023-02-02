package com.saurabhsandav.core.fyers_api.model.request

import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthValidationRequest(

    @SerialName("code")
    val code: String,

    @SerialName("appIdHash")
    val appIdHash: String,

    @SerialName("grant_type")
    val grantType: String,
)

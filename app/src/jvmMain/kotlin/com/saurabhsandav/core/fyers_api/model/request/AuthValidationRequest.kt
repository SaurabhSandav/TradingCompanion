package com.saurabhsandav.core.fyers_api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthValidationRequest(

    @SerialName("grant_type")
    val grantType: String,

    @SerialName("appIdHash")
    val appIdHash: String,

    @SerialName("code")
    val code: String,
)

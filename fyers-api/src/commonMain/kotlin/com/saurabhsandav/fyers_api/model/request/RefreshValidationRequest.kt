package com.saurabhsandav.fyers_api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RefreshValidationRequest(

    @SerialName("grant_type")
    val grantType: String,

    @SerialName("appIdHash")
    val appIdHash: String,

    @SerialName("refresh_token")
    val refreshToken: String,

    @SerialName("pin")
    val pin: String,
)

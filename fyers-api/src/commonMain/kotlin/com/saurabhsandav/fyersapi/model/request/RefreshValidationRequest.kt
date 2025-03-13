package com.saurabhsandav.fyersapi.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("ktlint:standard:no-blank-line-in-list")
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

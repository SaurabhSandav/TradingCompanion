package com.saurabhsandav.fyers_api.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("ktlint:standard:no-blank-line-in-list")
@Serializable
internal data class AuthValidationRequest(

    @SerialName("grant_type")
    val grantType: String,

    @SerialName("appIdHash")
    val appIdHash: String,

    @SerialName("code")
    val code: String,
)

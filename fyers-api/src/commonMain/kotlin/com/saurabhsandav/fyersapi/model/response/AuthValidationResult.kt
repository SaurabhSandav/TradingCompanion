package com.saurabhsandav.fyersapi.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("ktlint:standard:no-blank-line-in-list")
@Serializable
public data class AuthValidationResult(

    @SerialName("access_token")
    val accessToken: String,

    @SerialName("refresh_token")
    val refreshToken: String,
)

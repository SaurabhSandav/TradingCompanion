package com.saurabhsandav.fyers_api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class RefreshValidationResult(

    @SerialName("access_token")
    val accessToken: String,
)

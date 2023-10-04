package com.saurabhsandav.core.fyers_api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshValidationResult(

    @SerialName("access_token")
    val accessToken: String,
)

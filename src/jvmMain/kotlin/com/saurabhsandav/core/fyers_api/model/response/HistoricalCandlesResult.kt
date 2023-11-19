package com.saurabhsandav.core.fyers_api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoricalCandlesResult(

    @SerialName("candles")
    val candles: List<List<String>>,

    @SerialName("nextTime")
    val nextTime: Long? = null,
)

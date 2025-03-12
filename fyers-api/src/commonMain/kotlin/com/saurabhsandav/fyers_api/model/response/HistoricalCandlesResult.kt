package com.saurabhsandav.fyers_api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("ktlint:standard:no-blank-line-in-list")
@Serializable
public data class HistoricalCandlesResult(

    @SerialName("candles")
    val candles: List<List<String>>,

    @SerialName("nextTime")
    val nextTime: Long? = null,
)

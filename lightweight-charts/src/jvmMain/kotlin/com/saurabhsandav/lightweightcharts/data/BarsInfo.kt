package com.saurabhsandav.lightweightcharts.data

import kotlinx.serialization.Serializable

@Serializable
data class BarsInfo(
    val barsBefore: Float,
    val barsAfter: Float,
    val from: Time?,
    val to: Time?,
)

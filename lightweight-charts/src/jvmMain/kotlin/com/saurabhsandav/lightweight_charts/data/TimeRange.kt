package com.saurabhsandav.lightweight_charts.data

import kotlinx.serialization.Serializable

@Serializable
data class TimeRange(
    val from: Time,
    val to: Time,
)

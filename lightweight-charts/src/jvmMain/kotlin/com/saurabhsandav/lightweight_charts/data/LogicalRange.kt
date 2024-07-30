package com.saurabhsandav.lightweight_charts.data

import kotlinx.serialization.Serializable

@Serializable
data class LogicalRange(
    val from: Float,
    val to: Float,
)

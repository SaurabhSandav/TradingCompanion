package com.saurabhsandav.lightweight_charts.options

import kotlinx.serialization.Serializable

@Serializable
data class TimeScaleOptions(
    val timeVisible: Boolean? = null,
    val secondsVisible: Boolean? = null,
    val shiftVisibleRangeOnNewBar: Boolean? = null,
    val allowShiftVisibleRangeOnWhitespaceReplacement: Boolean? = null,
)

package com.saurabhsandav.lightweightcharts.options

import kotlinx.serialization.Serializable

@Serializable
data class TimeScaleOptions(
    val lockVisibleTimeRangeOnResize: Boolean? = null,
    val timeVisible: Boolean? = null,
    val secondsVisible: Boolean? = null,
    val shiftVisibleRangeOnNewBar: Boolean? = null,
    val allowShiftVisibleRangeOnWhitespaceReplacement: Boolean? = null,
)

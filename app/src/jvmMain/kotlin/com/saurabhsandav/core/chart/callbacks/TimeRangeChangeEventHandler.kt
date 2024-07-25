package com.saurabhsandav.core.chart.callbacks

import com.saurabhsandav.core.chart.misc.TimeRange

fun interface TimeRangeChangeEventHandler {

    fun onEvent(timeRange: TimeRange?)
}

package com.saurabhsandav.lightweight_charts.callbacks

import com.saurabhsandav.lightweight_charts.misc.TimeRange

fun interface TimeRangeChangeEventHandler {

    fun onEvent(timeRange: TimeRange?)
}

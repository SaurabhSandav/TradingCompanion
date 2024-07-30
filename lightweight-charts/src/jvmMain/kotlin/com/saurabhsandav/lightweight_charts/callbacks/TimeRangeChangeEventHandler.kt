package com.saurabhsandav.lightweight_charts.callbacks

import com.saurabhsandav.lightweight_charts.data.TimeRange

fun interface TimeRangeChangeEventHandler {

    fun onEvent(timeRange: TimeRange?)
}

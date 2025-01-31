package com.saurabhsandav.lightweight_charts.callbacks

import com.saurabhsandav.lightweight_charts.data.IRange
import com.saurabhsandav.lightweight_charts.data.Time

fun interface TimeRangeChangeEventHandler {

    fun onEvent(timeRange: IRange<Time>?)
}

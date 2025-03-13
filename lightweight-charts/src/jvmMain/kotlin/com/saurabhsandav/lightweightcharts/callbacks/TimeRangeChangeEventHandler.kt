package com.saurabhsandav.lightweightcharts.callbacks

import com.saurabhsandav.lightweightcharts.data.IRange
import com.saurabhsandav.lightweightcharts.data.Time

fun interface TimeRangeChangeEventHandler {

    fun onEvent(timeRange: IRange<Time>?)
}

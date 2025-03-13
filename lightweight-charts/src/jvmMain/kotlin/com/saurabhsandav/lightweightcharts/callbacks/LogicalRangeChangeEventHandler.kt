package com.saurabhsandav.lightweightcharts.callbacks

import com.saurabhsandav.lightweightcharts.data.LogicalRange

fun interface LogicalRangeChangeEventHandler {

    fun onEvent(range: LogicalRange?)
}

package com.saurabhsandav.lightweight_charts.callbacks

import com.saurabhsandav.lightweight_charts.data.LogicalRange

fun interface LogicalRangeChangeEventHandler {

    fun onEvent(range: LogicalRange?)
}

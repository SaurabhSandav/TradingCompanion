package com.saurabhsandav.lightweight_charts.callbacks

import com.saurabhsandav.lightweight_charts.misc.LogicalRange

fun interface LogicalRangeChangeEventHandler {

    fun onEvent(range: LogicalRange?)
}

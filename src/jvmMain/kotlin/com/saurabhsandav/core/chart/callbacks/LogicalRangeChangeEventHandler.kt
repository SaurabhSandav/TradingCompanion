package com.saurabhsandav.core.chart.callbacks

import com.saurabhsandav.core.chart.misc.LogicalRange

fun interface LogicalRangeChangeEventHandler {

    fun onEvent(range: LogicalRange?)
}

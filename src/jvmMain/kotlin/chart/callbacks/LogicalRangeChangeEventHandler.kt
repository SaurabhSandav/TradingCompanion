package chart.callbacks

import chart.misc.LogicalRange

fun interface LogicalRangeChangeEventHandler {

    fun onEvent(range: LogicalRange?)
}

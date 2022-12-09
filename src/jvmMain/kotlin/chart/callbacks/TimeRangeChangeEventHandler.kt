package chart.callbacks

import chart.misc.TimeRange

fun interface TimeRangeChangeEventHandler {

    fun onEvent(range: TimeRange?)
}

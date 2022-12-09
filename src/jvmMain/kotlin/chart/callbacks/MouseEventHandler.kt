package chart.callbacks

import chart.misc.MouseEventParams

fun interface MouseEventHandler {

    fun onEvent(params: MouseEventParams)
}

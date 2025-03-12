package com.saurabhsandav.lightweight_charts.callbacks

fun interface SizeChangeEventHandler {

    fun onEvent(
        width: Float,
        height: Float,
    )
}

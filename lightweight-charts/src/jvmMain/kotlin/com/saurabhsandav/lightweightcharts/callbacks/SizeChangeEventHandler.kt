package com.saurabhsandav.lightweightcharts.callbacks

fun interface SizeChangeEventHandler {

    fun onEvent(
        width: Float,
        height: Float,
    )
}

package com.saurabhsandav.lightweight_charts.callbacks

import com.saurabhsandav.lightweight_charts.misc.MouseEventParams

fun interface MouseEventHandler {

    fun onEvent(params: MouseEventParams)
}

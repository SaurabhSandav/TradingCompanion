package com.saurabhsandav.core.chart.callbacks

import com.saurabhsandav.core.chart.misc.MouseEventParams

fun interface MouseEventHandler {

    fun onEvent(params: MouseEventParams)
}

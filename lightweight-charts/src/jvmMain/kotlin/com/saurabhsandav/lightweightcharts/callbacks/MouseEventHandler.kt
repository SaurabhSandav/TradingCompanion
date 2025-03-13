package com.saurabhsandav.lightweightcharts.callbacks

import com.saurabhsandav.lightweightcharts.data.MouseEventParams

fun interface MouseEventHandler {

    fun onEvent(params: MouseEventParams)
}

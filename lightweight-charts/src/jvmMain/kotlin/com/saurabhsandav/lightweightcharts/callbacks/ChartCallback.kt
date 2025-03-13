package com.saurabhsandav.lightweightcharts.callbacks

data class ChartCallback(
    val chartId: String,
    val callbackType: String,
    val message: String,
)

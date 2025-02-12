package com.saurabhsandav.lightweight_charts.callbacks

data class ChartCallback(
    val chartId: String,
    val callbackType: String,
    val message: String,
)

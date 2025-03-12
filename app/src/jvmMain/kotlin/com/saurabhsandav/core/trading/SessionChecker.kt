package com.saurabhsandav.core.trading

fun interface SessionChecker {

    fun isSessionStart(
        candleSeries: CandleSeries,
        index: Int,
    ): Boolean
}

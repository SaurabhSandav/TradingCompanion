package com.saurabhsandav.core.trading.core

fun interface SessionChecker {

    fun isSessionStart(
        candleSeries: CandleSeries,
        index: Int,
    ): Boolean
}

package com.saurabhsandav.trading.core

fun interface SessionChecker {

    fun isSessionStart(
        candleSeries: CandleSeries,
        index: Int,
    ): Boolean
}

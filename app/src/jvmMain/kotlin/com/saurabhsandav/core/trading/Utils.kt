package com.saurabhsandav.core.trading

import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.SessionChecker
import com.saurabhsandav.trading.core.Timeframe

object DailySessionChecker : SessionChecker {

    override fun isSessionStart(
        candleSeries: CandleSeries,
        index: Int,
    ): Boolean {

        val timeframe = candleSeries.timeframe

        return when {
            // First candle is considered start of session for simplicity
            index == 0 -> true
            // There's no session at/after 1-day timeframe
            timeframe >= Timeframe.D1 -> false
            // If the time between last candle and current candle is greater than the timeframe,
            // current candle is considered start of a new session.
            else -> {
                val candleTime = candleSeries[index].openInstant
                val lastCandleTime = candleSeries[index - 1].openInstant
                (candleTime - lastCandleTime).inWholeSeconds > timeframe.seconds
            }
        }
    }
}

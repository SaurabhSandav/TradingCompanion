package com.saurabhsandav.trading.barreplay

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Candle

internal fun Candle.atState(state: BarReplay.CandleState): Candle {

    val isCandleBullish = close > open

    return when (state) {
        // Open
        BarReplay.CandleState.Open -> copy(
            high = open,
            low = open,
            close = open,
            volume = KBigDecimal.Zero,
        )

        // First Extreme
        // Bullish candle, update low first
        BarReplay.CandleState.Extreme1 if isCandleBullish -> copy(
            high = open,
            close = low,
            volume = KBigDecimal.Zero,
        )

        // First Extreme
        // Bearish candle, update high first
        BarReplay.CandleState.Extreme1 -> copy(
            low = open,
            close = high,
            volume = KBigDecimal.Zero,
        )

        // Second Extreme
        // Bullish candle, update high second
        BarReplay.CandleState.Extreme2 if isCandleBullish -> copy(close = high, volume = KBigDecimal.Zero)

        // Second Extreme
        // Bearish candle, update low second
        BarReplay.CandleState.Extreme2 -> copy(close = low, volume = KBigDecimal.Zero)

        // Close
        BarReplay.CandleState.Close -> this
    }
}

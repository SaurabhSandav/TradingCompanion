package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.core.Candle
import java.math.BigDecimal

internal fun Candle.atState(state: BarReplay.CandleState): Candle {

    val isCandleBullish = close > open

    return when (state) {
        // Open
        BarReplay.CandleState.Open -> copy(
            high = open,
            low = open,
            close = open,
            volume = BigDecimal.ZERO,
        )

        // First Extreme
        // Bullish candle, update low first
        BarReplay.CandleState.Extreme1 if isCandleBullish -> copy(
            high = open,
            close = low,
            volume = BigDecimal.ZERO,
        )

        // First Extreme
        // Bearish candle, update high first
        BarReplay.CandleState.Extreme1 -> copy(
            low = open,
            close = high,
            volume = BigDecimal.ZERO,
        )

        // Second Extreme
        // Bullish candle, update high second
        BarReplay.CandleState.Extreme2 if isCandleBullish -> copy(close = high, volume = BigDecimal.ZERO)

        // Second Extreme
        // Bearish candle, update low second
        BarReplay.CandleState.Extreme2 -> copy(close = low, volume = BigDecimal.ZERO)

        // Close
        BarReplay.CandleState.Close -> this
    }
}

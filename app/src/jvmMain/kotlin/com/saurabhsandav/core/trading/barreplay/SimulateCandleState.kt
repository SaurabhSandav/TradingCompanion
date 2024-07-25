package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.Candle
import java.math.BigDecimal

internal fun Candle.atState(state: BarReplay.CandleState): Candle {

    val isCandleBullish = close > open

    return when (state) {
        // Open
        BarReplay.CandleState.Open -> copy(
            high = open,
            low = open,
            close = open,
            volume = BigDecimal.ZERO
        )

        // First Extreme
        BarReplay.CandleState.Extreme1 -> when {
            // Bullish candle, update low first
            isCandleBullish -> copy(
                high = open,
                close = low,
                volume = BigDecimal.ZERO
            )

            // Bearish candle, update high first
            else -> copy(
                low = open,
                close = high,
                volume = BigDecimal.ZERO
            )
        }

        // Second Extreme
        BarReplay.CandleState.Extreme2 -> when {
            // Bullish candle, update high second
            isCandleBullish -> copy(close = high, volume = BigDecimal.ZERO)
            // Bearish candle, update low second
            else -> copy(close = low, volume = BigDecimal.ZERO)
        }

        // Close
        BarReplay.CandleState.Close -> this
    }
}

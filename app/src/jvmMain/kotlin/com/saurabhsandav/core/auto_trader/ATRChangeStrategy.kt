package com.saurabhsandav.core.auto_trader

import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.autotrader.Strategy
import com.saurabhsandav.core.trading.indicator.ATRIndicator
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import kotlin.time.Duration.Companion.minutes

fun atrChangeStrategy(atrMultiplier: BigDecimal = "1.5".toBigDecimal()) = Strategy(
    title = "ATR Change Strategy (> ${atrMultiplier.toPlainString()}x)",
) {

    val m5Series = getCandleSeries(Timeframe.M5).let(::requireNotNull)
    val dailySeries = getCandleSeries(Timeframe.D1).let(::requireNotNull)
    val m5Atr = ATRIndicator(m5Series)
    val m5Travel3 = TravelIndicator(m5Series, 3)

    val tz = TimeZone.currentSystemDefault()
    val preferredTradingStartTime = LocalTime(hour = 9, minute = 25)
    val preferredTradingEndTime = LocalTime(hour = 15, minute = 0)
    val exitPositionTime = LocalTime(hour = 15, minute = 15)

    var currentCandleInstant = m5Series.last().openInstant
    var cooldownFrom = Instant.DISTANT_PAST
    val cooldownDuration = 15.minutes

    Strategy.Instance {

        // Return if candle not finished
        if (currentCandleInstant == m5Series.last().openInstant) return@Instance
        currentCandleInstant = m5Series.last().openInstant

        val openLDT = m5Series.last().openInstant.toLocalDateTime(tz)

        if (openLDT.time >= exitPositionTime) exitAllPositions()

        if (hasOpenPositions) {
            cooldownFrom = m5Series.last().openInstant
            return@Instance
        }

        if (m5Series.last().openInstant < (cooldownFrom + cooldownDuration)) return@Instance

        cancelAllEntryOrders()

        // Return if outside preferred time or after square-off time
        val inPreferredTradingHours = preferredTradingStartTime < openLDT.time && openLDT.time < preferredTradingEndTime
        if (!inPreferredTradingHours) return@Instance

        // Proceed only if volume was rising for last 3 candles
        if (!m5Series.takeLast(4).dropLast(1).risingVolume()) return@Instance

        val lastCandleIndex = m5Series.lastIndex - 1
        val risingTrendM5 = m5Travel3[lastCandleIndex] > (m5Atr[lastCandleIndex - 1] * atrMultiplier)
        val risingTrend = risingTrendM5

        if (risingTrend) {

            val entry = m5Series[lastCandleIndex].close
            val stopSpread = m5Atr[lastCandleIndex]
            val stop = (entry - stopSpread).round()
            val target = (entry + ((entry - stop) * 1.5.toBigDecimal())).round()

            if (entry.compareTo(stop) == 0) return@Instance

            buy(
                price = entry,
                stop = stop,
                target = target,
            )
        }

        val fallingTrendM5 = m5Travel3[lastCandleIndex] < (m5Atr[lastCandleIndex - 1] * atrMultiplier).negate()
        val fallingTrend = fallingTrendM5

        if (fallingTrend) {

            val entry = m5Series[lastCandleIndex].close
            val stopSpread = m5Atr[lastCandleIndex]
            val stop = (entry + stopSpread).round()
            val target = (entry - ((stop - entry) * 1.5.toBigDecimal())).round()

            if (entry.compareTo(stop) == 0) return@Instance

            sell(
                price = entry,
                stop = stop,
                target = target,
            )
        }
    }
}

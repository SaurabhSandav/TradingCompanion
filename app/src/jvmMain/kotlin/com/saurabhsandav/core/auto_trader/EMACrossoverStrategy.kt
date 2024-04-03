package com.saurabhsandav.core.auto_trader

import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.autotrader.Strategy
import com.saurabhsandav.core.trading.indicator.ATRIndicator
import com.saurabhsandav.core.trading.indicator.ClosePriceIndicator
import com.saurabhsandav.core.trading.indicator.EMAIndicator
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

fun emaCrossoverStrategy() = Strategy(
    title = "EMA Crossover Strategy"
) {

    val m5Series = getCandleSeries(Timeframe.M5).let(::requireNotNull)
    val m5Atr = ATRIndicator(m5Series)
    val ema9 = EMAIndicator(ClosePriceIndicator(m5Series), 9)
    val ema20 = EMAIndicator(ClosePriceIndicator(m5Series), 20)

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

        val risingTrend = ema9[m5Series.lastIndex - 2] <= ema20[m5Series.lastIndex - 2] &&
                ema9[m5Series.lastIndex - 1] > ema20[m5Series.lastIndex - 1]

        if (risingTrend) {

            val entry = m5Series[m5Series.lastIndex - 1].close
            val stopSpread = m5Atr[m5Series.lastIndex - 1] * "1.5".toBigDecimal()
            val stop = (entry - stopSpread).round()
            val target = (entry + ((entry - stop) * 1.5.toBigDecimal())).round()

            if (entry.compareTo(stop) == 0) return@Instance

            buy(
                price = entry,
                stop = stop,
                target = target,
            )
        }

        val fallingTrend = ema9[m5Series.lastIndex - 2] >= ema20[m5Series.lastIndex - 2] &&
                ema9[m5Series.lastIndex - 1] < ema20[m5Series.lastIndex - 1]

        if (fallingTrend) {

            val entry = m5Series[m5Series.lastIndex - 1].close
            val stopSpread = m5Atr[m5Series.lastIndex - 1] * "1.5".toBigDecimal()
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

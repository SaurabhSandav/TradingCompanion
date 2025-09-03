package com.saurabhsandav.trading.record.stats

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.KRoundingMode
import com.saurabhsandav.kbigdecimal.isZero
import com.saurabhsandav.kbigdecimal.sumOf
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import com.saurabhsandav.trading.record.Trade
import com.saurabhsandav.trading.record.TradingRecord
import com.saurabhsandav.trading.record.stats.TradingStats.Drawdown
import com.saurabhsandav.trading.record.stats.TradingStats.PartialStatsKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.Duration

fun TradingRecord.buildStats(partialStatsKeys: List<PartialStatsKey> = emptyList()): Flow<TradingStats?> {
    return trades.allTrades.map { trades ->

        val builder = TradingStatsBuilder(partialStatsKeys)

        for (index in trades.indices.reversed()) {
            builder.recordTrade(trades[index])
        }

        builder.build()
    }
}

internal fun buildTradingStats(
    trades: List<Trade>,
    partialStatsKeys: List<PartialStatsKey> = emptyList(),
): TradingStats? {

    return TradingStatsBuilder(partialStatsKeys).run {
        recordTrades(trades)
        build()
    }
}

private class TradingStatsBuilder(
    private val partialStatsKeys: List<PartialStatsKey> = emptyList(),
) {

    private var count = 0
    private var pnl = KBigDecimal.Zero
    private var pnlNet = KBigDecimal.Zero
    private var pnlPeak = KBigDecimal.Zero
    private var pnlNetPeak = KBigDecimal.Zero
    private var fees = KBigDecimal.Zero
    private var duration = Duration.ZERO

    private var winCount = 0
    private var winPnl = KBigDecimal.Zero
    private var winPnlNet = KBigDecimal.Zero
    private var winFees = KBigDecimal.Zero
    private var winDuration = Duration.ZERO
    private var winLargest: KBigDecimal? = null
    private var winStreakLongest = 0
    private var winStreakCurrent = 0

    private var lossCount = 0
    private var lossPnl = KBigDecimal.Zero
    private var lossPnlNet = KBigDecimal.Zero
    private var lossFees = KBigDecimal.Zero
    private var lossDuration = Duration.ZERO
    private var lossLargest: KBigDecimal? = null
    private var lossStreakLongest = 0
    private var lossStreakCurrent = 0

    private val drawdowns = mutableListOf<Drawdown>()
    private var currentDrawdown: Drawdown? = null

    private val partialStatsBuilders = buildMap {
        partialStatsKeys.forEach { put(it, TradingStatsBuilder()) }
    }

    fun recordTrades(trades: List<Trade>) {
        trades.forEach(::recordTrade)
    }

    fun recordTrade(trade: Trade) {

        if (!trade.isClosed) return

        count++
        pnl += trade.pnl
        pnlNet += trade.netPnl
        pnlPeak = maxOf(pnlPeak, pnl)
        pnlNetPeak = maxOf(pnlNetPeak, pnlNet)
        fees += trade.fees
        duration += trade.duration

        if (trade.pnl > KBigDecimal.Zero) {
            winCount++
            winPnl += trade.pnl
            winPnlNet += trade.netPnl
            winFees += trade.fees
            winDuration += trade.duration
            val winLargestCurrent = winLargest
            winLargest = if (winLargestCurrent != null) maxOf(winLargestCurrent, trade.pnl) else trade.pnl
            winStreakCurrent++
            lossStreakCurrent = 0
            winStreakLongest = max(winStreakCurrent, winStreakLongest)
        }

        if (trade.pnl < KBigDecimal.Zero) {
            lossCount++
            lossPnl += trade.pnl
            lossPnlNet += trade.netPnl
            lossFees += trade.fees
            lossDuration += trade.duration
            val lossLargestCurrent = lossLargest
            lossLargest = if (lossLargestCurrent != null) minOf(lossLargestCurrent, trade.pnl) else trade.pnl
            lossStreakCurrent++
            winStreakCurrent = 0
            lossStreakLongest = max(lossStreakCurrent, lossStreakLongest)
        }

        calculateDrawdown(trade)

        partialStatsBuilders.forEach { (key, builder) ->
            if (key.shouldIncludeTrade(trade)) builder.recordTrade(trade)
        }
    }

    private fun calculateDrawdown(trade: Trade) {

        // If pnl hit a new peak, current drawdown (if any) has ended
        currentDrawdown
            ?.takeIf { it.pnlPeak != pnlPeak }
            ?.run {

                // Use the start of a new pnl peak as the end time for the current drawdown
                drawdowns += copy(
                    to = trade.entryTimestamp,
                    duration = trade.entryTimestamp - from,
                    pnlPeak = pnlPeak,
                    drawdown = drawdown,
                )

                currentDrawdown = null
            }

        // Pnl is less than peak, start new drawdown
        if (pnl < pnlPeak) {

            currentDrawdown = when (val currentDrawdown = currentDrawdown) {
                null -> Drawdown(
                    tradeCount = 1,
                    from = trade.entryTimestamp,
                    to = trade.exitTimestamp ?: trade.entryTimestamp,
                    duration = (trade.exitTimestamp ?: trade.entryTimestamp) - trade.entryTimestamp,
                    tradeIdFrom = trade.id,
                    tradeIdTo = trade.id,
                    pnlPeak = pnlPeak,
                    drawdown = (pnlPeak - pnl).negated(),
                )

                else -> currentDrawdown.copy(
                    tradeCount = currentDrawdown.tradeCount + 1,
                    to = trade.exitTimestamp ?: trade.entryTimestamp,
                    duration = (trade.exitTimestamp ?: trade.entryTimestamp) - currentDrawdown.from,
                    tradeIdTo = trade.id,
                    drawdown = minOf(currentDrawdown.drawdown, (pnlPeak - pnl).negated()),
                )
            }
        }
    }

    fun build(): TradingStats? {

        if (count == 0) return null

        val feesAverage = fees.roundedDiv(count.toKBigDecimal())
        val durationAverage = duration / count

        val winDecimal = winCount.toKBigDecimal().roundedDiv(count.toKBigDecimal())
        val winAverage = if (winCount == 0) null else winPnl.roundedDiv(winCount.toKBigDecimal())
        val winDurationAverage = if (winCount == 0) null else winDuration / winCount

        val lossDecimal = lossCount.toKBigDecimal().roundedDiv(count.toKBigDecimal())
        val lossAverage = if (lossCount == 0) null else lossPnl.roundedDiv(lossCount.toKBigDecimal())
        val lossDurationAverage = if (lossCount == 0) null else lossDuration / lossCount

        val profitFactor = when {
            winCount == 0 || lossCount == 0 -> null
            lossPnl.isZero() -> null
            else -> winPnl.roundedDiv(lossPnl)
        }

        val expectancy = when {
            winAverage == null || lossAverage == null -> null
            else -> (winDecimal * winAverage) + (lossDecimal * lossAverage)
        }

        val drawdowns = when (val currentDrawdown = currentDrawdown) {
            null -> drawdowns.toList()
            else -> drawdowns + currentDrawdown
        }

        val drawdownMax = drawdowns.minOfOrNull { it.drawdown }
        val drawdownAverage = drawdowns
            .takeIf { it.isNotEmpty() }
            ?.sumOf { it.drawdown }
            ?.roundedDiv(drawdowns.size.toKBigDecimal())

        val drawdownDurationMax = drawdowns.maxOfOrNull { it.duration }
        val drawdownDurationAverage = drawdowns
            .takeIf { it.isNotEmpty() }
            ?.fold(Duration.ZERO) { acc, drawdown -> acc + drawdown.duration }
            ?.div(drawdowns.size)

        return TradingStats(
            count = count,
            pnl = pnl,
            pnlNet = pnlNet,
            pnlPeak = pnlPeak,
            pnlNetPeak = pnlNetPeak,
            fees = fees,
            feesAverage = feesAverage,
            profitFactor = profitFactor,
            durationAverage = durationAverage,
            expectancy = expectancy,
            winCount = winCount,
            winPnl = winPnl,
            winPnlNet = winPnlNet,
            winFees = winFees,
            winPercent = (winDecimal * 100.toKBigDecimal()),
            winLargest = winLargest,
            winAverage = winAverage,
            winStreakLongest = winStreakLongest,
            winDurationAverage = winDurationAverage,
            lossCount = lossCount,
            lossPnl = lossPnl,
            lossPnlNet = lossPnlNet,
            lossFees = lossFees,
            lossPercent = (lossDecimal * 100.toKBigDecimal()),
            lossLargest = lossLargest,
            lossAverage = lossAverage,
            lossStreakLongest = lossStreakLongest,
            lossDurationAverage = lossDurationAverage,
            drawdowns = drawdowns,
            drawdownMax = drawdownMax,
            drawdownAverage = drawdownAverage,
            drawdownDurationMax = drawdownDurationMax,
            drawdownDurationAverage = drawdownDurationAverage,
            partialStats = partialStatsBuilders.mapValues { (_, builder) -> builder.build() },
        )
    }

    private val Trade.duration: Duration
        get() = (exitTimestamp ?: Clock.System.now()) - entryTimestamp

    private fun KBigDecimal.roundedDiv(other: KBigDecimal) = div(other, 4, KRoundingMode.HalfEven)
}

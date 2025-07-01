package com.saurabhsandav.trading.record.stats

import com.saurabhsandav.trading.record.Trade
import com.saurabhsandav.trading.record.TradingRecord
import com.saurabhsandav.trading.record.stats.TradingStats.Drawdown
import com.saurabhsandav.trading.record.stats.TradingStats.PartialStatsKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
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
    private var pnl = BigDecimal.ZERO
    private var pnlNet = BigDecimal.ZERO
    private var pnlPeak = BigDecimal.ZERO
    private var pnlNetPeak = BigDecimal.ZERO
    private var fees = BigDecimal.ZERO
    private var duration = Duration.ZERO

    private var winCount = 0
    private var winPnl = BigDecimal.ZERO
    private var winPnlNet = BigDecimal.ZERO
    private var winFees = BigDecimal.ZERO
    private var winDuration = Duration.ZERO
    private var winLargest: BigDecimal? = null
    private var winStreakLongest = 0
    private var winStreakCurrent = 0

    private var lossCount = 0
    private var lossPnl = BigDecimal.ZERO
    private var lossPnlNet = BigDecimal.ZERO
    private var lossFees = BigDecimal.ZERO
    private var lossDuration = Duration.ZERO
    private var lossLargest: BigDecimal? = null
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

        if (trade.pnl > BigDecimal.ZERO) {
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

        if (trade.pnl < BigDecimal.ZERO) {
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
                    pnlPeak = pnlPeak.stripTrailingZeros(),
                    drawdown = drawdown.stripTrailingZeros(),
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
                    drawdown = (pnlPeak - pnl).negate(),
                )

                else -> currentDrawdown.copy(
                    tradeCount = currentDrawdown.tradeCount + 1,
                    to = trade.exitTimestamp ?: trade.entryTimestamp,
                    duration = (trade.exitTimestamp ?: trade.entryTimestamp) - currentDrawdown.from,
                    tradeIdTo = trade.id,
                    drawdown = minOf(currentDrawdown.drawdown, (pnlPeak - pnl).negate()),
                )
            }
        }
    }

    fun build(): TradingStats? {

        if (count == 0) return null

        val feesAverage = fees.roundedDiv(count.toBigDecimal())
        val durationAverage = duration / count

        val winDecimal = winCount.toBigDecimal().roundedDiv(count.toBigDecimal())
        val winAverage = if (winCount == 0) null else winPnl.roundedDiv(winCount.toBigDecimal())
        val winDurationAverage = if (winCount == 0) null else winDuration / winCount

        val lossDecimal = lossCount.toBigDecimal().roundedDiv(count.toBigDecimal())
        val lossAverage = if (lossCount == 0) null else lossPnl.roundedDiv(lossCount.toBigDecimal())
        val lossDurationAverage = if (lossCount == 0) null else lossDuration / lossCount

        val profitFactor = when {
            winCount == 0 || lossCount == 0 -> null
            lossPnl.compareTo(BigDecimal.ZERO) == 0 -> null
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
            ?.roundedDiv(drawdowns.size.toBigDecimal())

        val drawdownDurationMax = drawdowns.maxOfOrNull { it.duration }
        val drawdownDurationAverage = drawdowns
            .takeIf { it.isNotEmpty() }
            ?.fold(Duration.ZERO) { acc, drawdown -> acc + drawdown.duration }
            ?.div(drawdowns.size)

        return TradingStats(
            count = count,
            pnl = pnl.stripTrailingZeros(),
            pnlNet = pnlNet.stripTrailingZeros(),
            pnlPeak = pnlPeak.stripTrailingZeros(),
            pnlNetPeak = pnlNetPeak.stripTrailingZeros(),
            fees = fees.stripTrailingZeros(),
            feesAverage = feesAverage.stripTrailingZeros(),
            profitFactor = profitFactor?.stripTrailingZeros(),
            durationAverage = durationAverage,
            expectancy = expectancy?.stripTrailingZeros(),
            winCount = winCount,
            winPnl = winPnl.stripTrailingZeros(),
            winPnlNet = winPnlNet.stripTrailingZeros(),
            winFees = winFees.stripTrailingZeros(),
            winPercent = (winDecimal * 100.toBigDecimal()).stripTrailingZeros(),
            winLargest = winLargest?.stripTrailingZeros(),
            winAverage = winAverage?.stripTrailingZeros(),
            winStreakLongest = winStreakLongest,
            winDurationAverage = winDurationAverage,
            lossCount = lossCount,
            lossPnl = lossPnl.stripTrailingZeros(),
            lossPnlNet = lossPnlNet.stripTrailingZeros(),
            lossFees = lossFees.stripTrailingZeros(),
            lossPercent = (lossDecimal * 100.toBigDecimal()).stripTrailingZeros(),
            lossLargest = lossLargest?.stripTrailingZeros(),
            lossAverage = lossAverage?.stripTrailingZeros(),
            lossStreakLongest = lossStreakLongest,
            lossDurationAverage = lossDurationAverage,
            drawdowns = drawdowns,
            drawdownMax = drawdownMax?.stripTrailingZeros(),
            drawdownAverage = drawdownAverage?.stripTrailingZeros(),
            drawdownDurationMax = drawdownDurationMax,
            drawdownDurationAverage = drawdownDurationAverage,
            partialStats = partialStatsBuilders.mapValues { (_, builder) -> builder.build() },
        )
    }

    private val Trade.duration: Duration
        get() = (exitTimestamp ?: Clock.System.now()) - entryTimestamp

    private fun BigDecimal.roundedDiv(other: BigDecimal) = divide(other, 4, RoundingMode.HALF_EVEN)
}

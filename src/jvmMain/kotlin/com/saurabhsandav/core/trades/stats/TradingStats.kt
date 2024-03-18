package com.saurabhsandav.core.trades.stats

import com.saurabhsandav.core.trades.Trade
import java.math.BigDecimal
import kotlin.time.Duration

data class TradingStats(
    val count: Int,
    val pnl: BigDecimal,
    val pnlNet: BigDecimal,
    val pnlPeak: BigDecimal,
    val pnlNetPeak: BigDecimal,
    val fees: BigDecimal,
    val feesAverage: BigDecimal,
    val profitFactor: BigDecimal?,
    val durationAverage: Duration,
    val expectancy: BigDecimal?,
    val winCount: Int,
    val winPnl: BigDecimal,
    val winPnlNet: BigDecimal,
    val winFees: BigDecimal,
    val winPercent: BigDecimal,
    val winLargest: BigDecimal?,
    val winAverage: BigDecimal?,
    val winStreakLongest: Int,
    val winDurationAverage: Duration?,
    val lossCount: Int,
    val lossPnl: BigDecimal,
    val lossPnlNet: BigDecimal,
    val lossFees: BigDecimal,
    val lossPercent: BigDecimal,
    val lossLargest: BigDecimal?,
    val lossAverage: BigDecimal?,
    val lossStreakLongest: Int,
    val lossDurationAverage: Duration?,
    val partialStats: Map<PartialStatsKey, TradingStats?>,
) {

    interface PartialStatsKey {

        fun shouldIncludeTrade(trade: Trade): Boolean
    }
}

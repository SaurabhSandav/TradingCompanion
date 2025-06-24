package com.saurabhsandav.core.trading.record.stats

import com.saurabhsandav.core.trading.record.Trade
import com.saurabhsandav.core.trading.record.model.TradeId
import java.math.BigDecimal
import kotlin.time.Duration
import kotlin.time.Instant

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
    val drawdowns: List<Drawdown>,
    val drawdownMax: BigDecimal?,
    val drawdownAverage: BigDecimal?,
    val drawdownDurationMax: Duration?,
    val drawdownDurationAverage: Duration?,
    val partialStats: Map<PartialStatsKey, TradingStats?>,
) {

    interface PartialStatsKey {

        fun shouldIncludeTrade(trade: Trade): Boolean
    }

    data class Drawdown(
        val tradeCount: Int,
        val from: Instant,
        val to: Instant,
        val duration: Duration,
        val tradeIdFrom: TradeId,
        val tradeIdTo: TradeId,
        val pnlPeak: BigDecimal,
        val drawdown: BigDecimal,
    )
}

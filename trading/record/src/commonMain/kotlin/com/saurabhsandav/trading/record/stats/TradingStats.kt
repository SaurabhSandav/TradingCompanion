package com.saurabhsandav.trading.record.stats

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.record.Trade
import com.saurabhsandav.trading.record.model.TradeId
import kotlin.time.Duration
import kotlin.time.Instant

data class TradingStats(
    val count: Int,
    val pnl: KBigDecimal,
    val pnlNet: KBigDecimal,
    val pnlPeak: KBigDecimal,
    val pnlNetPeak: KBigDecimal,
    val fees: KBigDecimal,
    val feesAverage: KBigDecimal,
    val profitFactor: KBigDecimal?,
    val durationAverage: Duration,
    val expectancy: KBigDecimal?,
    val winCount: Int,
    val winPnl: KBigDecimal,
    val winPnlNet: KBigDecimal,
    val winFees: KBigDecimal,
    val winPercent: KBigDecimal,
    val winLargest: KBigDecimal?,
    val winAverage: KBigDecimal?,
    val winStreakLongest: Int,
    val winDurationAverage: Duration?,
    val lossCount: Int,
    val lossPnl: KBigDecimal,
    val lossPnlNet: KBigDecimal,
    val lossFees: KBigDecimal,
    val lossPercent: KBigDecimal,
    val lossLargest: KBigDecimal?,
    val lossAverage: KBigDecimal?,
    val lossStreakLongest: Int,
    val lossDurationAverage: Duration?,
    val drawdowns: List<Drawdown>,
    val drawdownMax: KBigDecimal?,
    val drawdownAverage: KBigDecimal?,
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
        val pnlPeak: KBigDecimal,
        val drawdown: KBigDecimal,
    )
}

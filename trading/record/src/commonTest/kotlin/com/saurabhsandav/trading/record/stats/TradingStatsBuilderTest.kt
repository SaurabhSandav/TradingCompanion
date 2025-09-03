package com.saurabhsandav.trading.record.stats

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.record.Trade
import com.saurabhsandav.trading.test.assertBDEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class TradingStatsBuilderTest {

    @Test
    fun `No trades`() {

        val stats = buildTradingStats(emptyList())

        assertNull(stats)
    }

    @Test
    fun `All trades`() {

        val stats = buildTradingStats(TradingStatsUtils.trades)

        assertNotNull(stats)

        assertEquals(20, stats.count)
        assertBDEquals("-708.25", stats.pnl)
        assertBDEquals("-975.32", stats.pnlNet)
        assertBDEquals("514.25", stats.pnlPeak)
        assertBDEquals("328.23", stats.pnlNetPeak)
        assertBDEquals("267.07", stats.fees)
        assertBDEquals("13.3535", stats.feesAverage)
        assertBDEquals("-0.7479", stats.profitFactor)
        assertEquals(1975.55.seconds, stats.durationAverage)
        assertBDEquals("-35.41248", stats.expectancy)
        assertEquals(6, stats.winCount)
        assertBDEquals(2101, stats.winPnl)
        assertBDEquals("2011.44", stats.winPnlNet)
        assertBDEquals("89.56", stats.winFees)
        assertBDEquals(30, stats.winPercent)
        assertBDEquals(1092, stats.winLargest)
        assertBDEquals("350.1667", stats.winAverage)
        assertEquals(3, stats.winStreakLongest)
        assertEquals(2875.5.seconds, stats.winDurationAverage)
        assertEquals(14, stats.lossCount)
        assertBDEquals("-2809.25", stats.lossPnl)
        assertBDEquals("-2986.76", stats.lossPnlNet)
        assertBDEquals("177.51", stats.lossFees)
        assertBDEquals(70, stats.lossPercent)
        assertBDEquals(-280, stats.lossLargest)
        assertBDEquals("-200.6607", stats.lossAverage)
        assertEquals(7, stats.lossStreakLongest)
        assertEquals(1589.857142857.seconds, stats.lossDurationAverage)
    }

    @Test
    fun `Wins only`() {

        val stats = buildTradingStats(TradingStatsUtils.winTrades)

        assertNotNull(stats)

        assertEquals(6, stats.count)
        assertBDEquals(2101, stats.pnl)
        assertBDEquals("2011.44", stats.pnlNet)
        assertBDEquals(2101, stats.pnlPeak)
        assertBDEquals("2011.44", stats.pnlNetPeak)
        assertBDEquals("89.56", stats.fees)
        assertBDEquals("14.9267", stats.feesAverage)
        assertNull(stats.profitFactor)
        assertEquals(2875.5.seconds, stats.durationAverage)
        assertNull(stats.expectancy)
        assertEquals(6, stats.winCount)
        assertBDEquals(2101, stats.winPnl)
        assertBDEquals("2011.44", stats.winPnlNet)
        assertBDEquals("89.56", stats.winFees)
        assertBDEquals(100, stats.winPercent)
        assertBDEquals(1092, stats.winLargest)
        assertBDEquals("350.1667", stats.winAverage)
        assertEquals(6, stats.winStreakLongest)
        assertEquals(2875.5.seconds, stats.winDurationAverage)
        assertEquals(0, stats.lossCount)
        assertBDEquals(0, stats.lossPnl)
        assertBDEquals(0, stats.lossPnlNet)
        assertBDEquals(0, stats.lossFees)
        assertBDEquals(0, stats.lossPercent)
        assertNull(stats.lossLargest)
        assertNull(stats.lossAverage)
        assertEquals(0, stats.lossStreakLongest)
        assertNull(stats.lossDurationAverage)
    }

    @Test
    fun `Losses only`() {

        val stats = buildTradingStats(TradingStatsUtils.lossTrades)

        assertNotNull(stats)

        assertEquals(14, stats.count)
        assertBDEquals("-2809.25", stats.pnl)
        assertBDEquals("-2986.76", stats.pnlNet)
        assertBDEquals(0, stats.pnlPeak)
        assertBDEquals(0, stats.pnlNetPeak)
        assertBDEquals("177.51", stats.fees)
        assertBDEquals("12.6793", stats.feesAverage)
        assertNull(stats.profitFactor)
        assertEquals(1589.857142857.seconds, stats.durationAverage)
        assertNull(stats.expectancy)
        assertEquals(0, stats.winCount)
        assertBDEquals(0, stats.winPnl)
        assertBDEquals(0, stats.winPnlNet)
        assertBDEquals(0, stats.winFees)
        assertBDEquals(0, stats.winPercent)
        assertNull(stats.winLargest)
        assertNull(stats.winAverage)
        assertEquals(0, stats.winStreakLongest)
        assertNull(stats.winDurationAverage)
        assertEquals(14, stats.lossCount)
        assertBDEquals("-2809.25", stats.lossPnl)
        assertBDEquals("-2986.76", stats.lossPnlNet)
        assertBDEquals("177.51", stats.lossFees)
        assertBDEquals(100, stats.lossPercent)
        assertBDEquals(-280, stats.lossLargest)
        assertBDEquals("-200.6607", stats.lossAverage)
        assertEquals(14, stats.lossStreakLongest)
        assertEquals(1589.857142857.seconds, stats.lossDurationAverage)
    }

    @Suppress("TestFunctionName", "ktlint:standard:function-naming")
    @Test
    fun Drawdowns() {

        val stats = buildTradingStats(TradingStatsUtils.trades)

        assertNotNull(stats)

        assertEquals(3, stats.drawdowns.size)
        assertBDEquals("-1222.5", stats.drawdownMax)
        assertBDEquals("-936.4167", stats.drawdownAverage)
        assertEquals(85501, stats.drawdownDurationMax?.inWholeSeconds)
        assertEquals(35356, stats.drawdownDurationAverage?.inWholeSeconds)

        assertEquals(5, stats.drawdowns[0].tradeCount)
        assertEquals(1706503676, stats.drawdowns[0].from.epochSeconds)
        assertEquals(1706516947, stats.drawdowns[0].to.epochSeconds)
        assertEquals(332, stats.drawdowns[0].tradeIdFrom.value)
        assertEquals(336, stats.drawdowns[0].tradeIdTo.value)
        assertBDEquals(200, stats.drawdowns[0].pnlPeak)
        assertBDEquals("-515.25", stats.drawdowns[0].drawdown)

        assertEquals(4, stats.drawdowns[1].tradeCount)
        assertEquals(1706589353, stats.drawdowns[1].from.epochSeconds)
        assertEquals(1706596651, stats.drawdowns[1].to.epochSeconds)
        assertEquals(338, stats.drawdowns[1].tradeIdFrom.value)
        assertEquals(341, stats.drawdowns[1].tradeIdTo.value)
        assertBDEquals("407.75", stats.drawdowns[1].pnlPeak)
        assertBDEquals("-1071.5", stats.drawdowns[1].drawdown)

        assertEquals(7, stats.drawdowns[2].tradeCount)
        assertEquals(1706680896, stats.drawdowns[2].from.epochSeconds)
        assertEquals(1706766397, stats.drawdowns[2].to.epochSeconds)
        assertEquals(344, stats.drawdowns[2].tradeIdFrom.value)
        assertEquals(350, stats.drawdowns[2].tradeIdTo.value)
        assertBDEquals("514.25", stats.drawdowns[2].pnlPeak)
        assertBDEquals("-1222.5", stats.drawdowns[2].drawdown)
    }

    @Test
    fun `All trades with partial stats`() {

        val winnersKey = object : TradingStats.PartialStatsKey {
            override fun shouldIncludeTrade(trade: Trade): Boolean = trade.pnl > KBigDecimal.Zero
        }

        val losersKey = object : TradingStats.PartialStatsKey {
            override fun shouldIncludeTrade(trade: Trade): Boolean = trade.pnl < KBigDecimal.Zero
        }

        val stats = buildTradingStats(TradingStatsUtils.trades, listOf(winnersKey, losersKey))

        assertNotNull(stats)

        assertEquals(20, stats.count)
        assertBDEquals("-708.25", stats.pnl)
        assertBDEquals("-975.32", stats.pnlNet)
        assertBDEquals("514.25", stats.pnlPeak)
        assertBDEquals("328.23", stats.pnlNetPeak)
        assertBDEquals("267.07", stats.fees)
        assertBDEquals("13.3535", stats.feesAverage)
        assertBDEquals("-0.7479", stats.profitFactor)
        assertEquals(1975.55.seconds, stats.durationAverage)
        assertBDEquals("-35.41248", stats.expectancy)
        assertEquals(6, stats.winCount)
        assertBDEquals(2101, stats.winPnl)
        assertBDEquals("2011.44", stats.winPnlNet)
        assertBDEquals("89.56", stats.winFees)
        assertBDEquals(30, stats.winPercent)
        assertBDEquals(1092, stats.winLargest)
        assertBDEquals("350.1667", stats.winAverage)
        assertEquals(3, stats.winStreakLongest)
        assertEquals(2875.5.seconds, stats.winDurationAverage)
        assertEquals(14, stats.lossCount)
        assertBDEquals("-2809.25", stats.lossPnl)
        assertBDEquals("-2986.76", stats.lossPnlNet)
        assertBDEquals("177.51", stats.lossFees)
        assertBDEquals(70, stats.lossPercent)
        assertBDEquals(-280, stats.lossLargest)
        assertBDEquals("-200.6607", stats.lossAverage)
        assertEquals(7, stats.lossStreakLongest)
        assertEquals(1589.857142857.seconds, stats.lossDurationAverage)

        assertEquals(stats.partialStats.size, 2)

        val winStats = assertNotNull(stats.partialStats[winnersKey])

        assertEquals(6, winStats.count)
        assertBDEquals(2101, winStats.pnl)
        assertBDEquals("2011.44", winStats.pnlNet)
        assertBDEquals(2101, winStats.pnlPeak)
        assertBDEquals("2011.44", winStats.pnlNetPeak)
        assertBDEquals("89.56", winStats.fees)
        assertBDEquals("14.9267", winStats.feesAverage)
        assertNull(winStats.profitFactor)
        assertEquals(2875.5.seconds, winStats.durationAverage)
        assertNull(winStats.expectancy)
        assertEquals(6, winStats.winCount)
        assertBDEquals(2101, winStats.winPnl)
        assertBDEquals("2011.44", winStats.winPnlNet)
        assertBDEquals("89.56", winStats.winFees)
        assertBDEquals(100, winStats.winPercent)
        assertBDEquals(1092, winStats.winLargest)
        assertBDEquals("350.1667", winStats.winAverage)
        assertEquals(6, winStats.winStreakLongest)
        assertEquals(2875.5.seconds, winStats.winDurationAverage)
        assertEquals(0, winStats.lossCount)
        assertBDEquals(0, winStats.lossPnl)
        assertBDEquals(0, winStats.lossPnlNet)
        assertBDEquals(0, winStats.lossFees)
        assertBDEquals(0, winStats.lossPercent)
        assertNull(winStats.lossLargest)
        assertNull(winStats.lossAverage)
        assertEquals(0, winStats.lossStreakLongest)
        assertNull(winStats.lossDurationAverage)

        val lossStats = assertNotNull(stats.partialStats[losersKey])

        assertEquals(14, lossStats.count)
        assertBDEquals("-2809.25", lossStats.pnl)
        assertBDEquals("-2986.76", lossStats.pnlNet)
        assertBDEquals(0, lossStats.pnlPeak)
        assertBDEquals(0, lossStats.pnlNetPeak)
        assertBDEquals("177.51", lossStats.fees)
        assertBDEquals("12.6793", lossStats.feesAverage)
        assertNull(lossStats.profitFactor)
        assertEquals(1589.857142857.seconds, lossStats.durationAverage)
        assertNull(lossStats.expectancy)
        assertEquals(0, lossStats.winCount)
        assertBDEquals(0, lossStats.winPnl)
        assertBDEquals(0, lossStats.winPnlNet)
        assertBDEquals(0, lossStats.winFees)
        assertBDEquals(0, lossStats.winPercent)
        assertNull(lossStats.winLargest)
        assertNull(lossStats.winAverage)
        assertEquals(0, lossStats.winStreakLongest)
        assertNull(lossStats.winDurationAverage)
        assertEquals(14, lossStats.lossCount)
        assertBDEquals("-2809.25", lossStats.lossPnl)
        assertBDEquals("-2986.76", lossStats.lossPnlNet)
        assertBDEquals("177.51", lossStats.lossFees)
        assertBDEquals(100, lossStats.lossPercent)
        assertBDEquals(-280, lossStats.lossLargest)
        assertBDEquals("-200.6607", lossStats.lossAverage)
        assertEquals(14, lossStats.lossStreakLongest)
        assertEquals(1589.857142857.seconds, lossStats.lossDurationAverage)
    }
}

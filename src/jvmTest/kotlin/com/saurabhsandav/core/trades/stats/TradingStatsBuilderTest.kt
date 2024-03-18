package com.saurabhsandav.core.trades.stats

import com.saurabhsandav.core.assertBDEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class TradingStatsBuilderTest {

    // TODO Replace hardcoded broker Finvasia with a fake broker

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
}

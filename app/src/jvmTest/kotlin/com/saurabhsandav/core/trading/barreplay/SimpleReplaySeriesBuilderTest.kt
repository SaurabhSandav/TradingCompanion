package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.trading.core.Timeframe
import com.saurabhsandav.trading.test.CandleUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class SimpleReplaySeriesBuilderTest {

    @Test
    fun `Initial state`() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            initialIndex = 200,
        )

        assertEquals(CandleUtils.m5Series[199], sut.replaySeries.last())
        assertEquals(200, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[199].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Initial state daily`() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = CandleUtils.d1Series,
            initialIndex = 200,
        )

        assertEquals(CandleUtils.d1Series[199], sut.replaySeries.last())
        assertEquals(200, sut.replaySeries.size)
        assertEquals(CandleUtils.d1Series[199].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Initial state with BarReplay`() {

        val barReplay = BarReplay(
            timeframe = Timeframe.M5,
            from = CandleUtils.m5Series[200].openInstant - 1.seconds,
        )

        val sut = barReplay.newSeries(inputSeries = CandleUtils.m5Series)

        assertEquals(CandleUtils.m5Series[199], sut.last())
        assertEquals(200, sut.size)
        assertEquals(CandleUtils.m5Series[199].openInstant, sut.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.candleState.value)
    }

    @Test
    fun `Initial state with already advanced BarReplay`() {

        val barReplay = BarReplay(
            timeframe = Timeframe.M5,
            from = CandleUtils.m5Series[200].openInstant - 1.seconds,
        )

        barReplay.newSeries(inputSeries = CandleUtils.m5Series)

        repeat(5) {
            barReplay.advance()
        }

        val sut = barReplay.newSeries(inputSeries = CandleUtils.m5Series)

        assertEquals(CandleUtils.m5Series[204], sut.last())
        assertEquals(205, sut.size)
        assertEquals(CandleUtils.m5Series[204].openInstant, sut.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.candleState.value)
    }

    @Test
    fun `Initial state with already advanced BarReplay with partial candle`() {

        val barReplay = BarReplay(
            timeframe = Timeframe.M5,
            from = CandleUtils.m5Series[200].openInstant - 1.seconds,
            candleUpdateType = CandleUpdateType.OHLC,
        )

        barReplay.newSeries(inputSeries = CandleUtils.m5Series)

        repeat(5) {
            barReplay.advance()
        }

        val sut = barReplay.newSeries(inputSeries = CandleUtils.m5Series)

        assertEquals(CandleUtils.m5Series[201].atState(BarReplay.CandleState.Open), sut.last())
        assertEquals(202, sut.size)
        assertEquals(CandleUtils.m5Series[201].openInstant, sut.replayTime.value)
        assertEquals(BarReplay.CandleState.Open, sut.candleState.value)
    }

    @Test
    fun `Advance to closed candle`() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            initialIndex = 200,
        )

        sut.advanceTo(CandleUtils.m5Series[203].openInstant, BarReplay.CandleState.Close)

        assertEquals(CandleUtils.m5Series[203], sut.replaySeries.last())
        assertEquals(204, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[203].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Advance to closed candle daily`() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = CandleUtils.d1Series,
            initialIndex = 200,
        )

        sut.advanceTo(CandleUtils.d1Series[203].openInstant, BarReplay.CandleState.Close)

        assertEquals(CandleUtils.d1Series[203], sut.replaySeries.last())
        assertEquals(204, sut.replaySeries.size)
        assertEquals(CandleUtils.d1Series[203].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Skip to partial candle`() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            initialIndex = 200,
        )

        sut.advanceTo(CandleUtils.m5Series[203].openInstant, BarReplay.CandleState.Extreme2)

        // Check previous candles were closed
        sut.replaySeries.subList(200, 203).forEachIndexed { index, candle ->
            assertEquals(CandleUtils.m5Series[200 + index], candle)
        }

        assertEquals(CandleUtils.m5Series[203].atState(BarReplay.CandleState.Extreme2), sut.replaySeries.last())
        assertEquals(204, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[203].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Extreme2, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Advance sequentially to partial candle`() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            initialIndex = 200,
        )

        sut.advanceTo(CandleUtils.m5Series[200].openInstant, BarReplay.CandleState.Open)
        sut.advanceTo(CandleUtils.m5Series[200].openInstant, BarReplay.CandleState.Extreme1)
        sut.advanceTo(CandleUtils.m5Series[200].openInstant, BarReplay.CandleState.Extreme2)

        assertEquals(CandleUtils.m5Series[200].atState(BarReplay.CandleState.Extreme2), sut.replaySeries.last())
        assertEquals(201, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[200].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Extreme2, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Reset after closed candle`() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            initialIndex = 200,
        )

        sut.advanceTo(CandleUtils.m5Series[203].openInstant, BarReplay.CandleState.Close)
        sut.reset()

        assertEquals(CandleUtils.m5Series[199], sut.replaySeries.last())
        assertEquals(200, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[199].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Reset after partial candle`() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            initialIndex = 200,
        )

        sut.advanceTo(CandleUtils.m5Series[203].openInstant, BarReplay.CandleState.Open)
        sut.reset()

        assertEquals(CandleUtils.m5Series[199], sut.replaySeries.last())
        assertEquals(200, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[199].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }
}

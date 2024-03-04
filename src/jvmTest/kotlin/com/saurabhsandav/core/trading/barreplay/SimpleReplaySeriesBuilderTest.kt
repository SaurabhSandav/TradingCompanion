package com.saurabhsandav.core.trading.barreplay

import kotlin.test.Test
import kotlin.test.assertEquals

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

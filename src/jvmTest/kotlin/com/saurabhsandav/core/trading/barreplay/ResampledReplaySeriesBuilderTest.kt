package com.saurabhsandav.core.trading.barreplay

import kotlin.test.Test
import kotlin.test.assertEquals

class ResampledReplaySeriesBuilderTest {

    @Test
    fun `Initial state`() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
            initialIndex = 200,
        )

        assertEquals(CandleUtils.m15Series[199], sut.replaySeries.last())
        assertEquals(200, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[199].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Initial state partially resampled candle`() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
            initialIndex = 201,
        )

        assertEquals(CandleUtils.m5Series[200], sut.replaySeries.last())
        assertEquals(201, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[200].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Advance to closed candle`() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
            initialIndex = 200,
        )

        sut.advanceTo(CandleUtils.m5Series[211].openInstant, BarReplay.CandleState.Close)

        assertEquals(CandleUtils.m15Series[203], sut.replaySeries.last())
        assertEquals(204, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[211].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Skip to partial candle`() {

        val candleState = BarReplay.CandleState.Extreme2

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
            initialIndex = 200,
        )

        sut.advanceTo(CandleUtils.m5Series[209].openInstant, candleState)

        assertEquals(CandleUtils.m5Series[209].atState(candleState), sut.replaySeries.last())
        assertEquals(204, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[209].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(candleState, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Advance sequentially to partial candle`() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
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

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
            initialIndex = 200,
        )

        sut.advanceTo(CandleUtils.m5Series[211].openInstant, BarReplay.CandleState.Close)
        sut.reset()

        assertEquals(CandleUtils.m15Series[199], sut.replaySeries.last())
        assertEquals(200, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[199].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun `Reset after partial candle`() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
            initialIndex = 200,
        )

        sut.advanceTo(CandleUtils.m5Series[209].openInstant, BarReplay.CandleState.Open)
        sut.reset()

        assertEquals(CandleUtils.m15Series[199], sut.replaySeries.last())
        assertEquals(200, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[199].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }
}

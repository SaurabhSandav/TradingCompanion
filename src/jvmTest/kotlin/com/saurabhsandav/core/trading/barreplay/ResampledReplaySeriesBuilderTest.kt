package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.Timeframe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

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
    fun `Initial state daily`() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.d1Series,
            initialIndex = 200,
        )

        assertEquals(CandleUtils.d1Series[199], sut.replaySeries.last())
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
    fun `Initial state with BarReplay`() {

        val barReplay = BarReplay(
            timeframe = Timeframe.M5,
            from = CandleUtils.m5Series[200].openInstant - 1.seconds,
        )

        val sut = barReplay.newSeries(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
        )

        assertEquals(CandleUtils.m15Series[199], sut.last())
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

        barReplay.newSeries(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
        )

        repeat(5) {
            barReplay.advance()
        }

        val sut = barReplay.newSeries(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
        )

        assertEquals(CandleUtils.m5Series.subList(203, 205).reduce(CandleUtils::resample), sut.last())
        assertEquals(202, sut.size)
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

        barReplay.newSeries(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
        )

        repeat(18) {
            barReplay.advance()
        }

        val sut = barReplay.newSeries(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.m15Series,
        )

        assertEquals(
            expected = CandleUtils.m5Series
                .subList(203, 205)
                .reduceIndexed { i, resampledCandle, newCandle ->
                    val stateCandle = if (i == 1) newCandle.atState(BarReplay.CandleState.Extreme1) else newCandle
                    CandleUtils.resample(resampledCandle, stateCandle)
                },
            actual = sut.last(),
        )
        assertEquals(202, sut.size)
        assertEquals(CandleUtils.m5Series[204].openInstant, sut.replayTime.value)
        assertEquals(BarReplay.CandleState.Extreme1, sut.candleState.value)
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
    fun `Advance to closed candle daily`() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = CandleUtils.m5Series,
            timeframeSeries = CandleUtils.d1Series,
            initialIndex = 200,
        )

        sut.advanceTo(CandleUtils.d1Series[201].openInstant, BarReplay.CandleState.Close)

        assertEquals(CandleUtils.d1Series[200], sut.replaySeries.last())
        assertEquals(201, sut.replaySeries.size)
        assertEquals(CandleUtils.m5Series[274].openInstant, sut.replaySeries.replayTime.value)
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

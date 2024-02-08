package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.asCandleSeries
import kotlinx.datetime.Instant
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleReplaySeriesBuilderTest {

    @Test
    fun init() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = inputSeries,
            initialIndex = 1,
        )

        assertEquals(inputSeries[0], sut.replaySeries.last())
        assertEquals(1, sut.replaySeries.size)
        assertEquals(inputSeries[0].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun advance_closed() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = inputSeries,
            initialIndex = 1,
        )

        sut.advanceTo(inputSeries[3].openInstant, BarReplay.CandleState.Close)

        assertEquals(inputSeries[3], sut.replaySeries.last())
        assertEquals(4, sut.replaySeries.size)
        assertEquals(inputSeries[3].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun advance_not_closed() {

        val candleState = BarReplay.CandleState.Extreme2

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = inputSeries,
            initialIndex = 1,
        )

        sut.advanceTo(inputSeries[1].openInstant, candleState)

        assertEquals(inputSeries[1].atState(candleState), sut.replaySeries.last())
        assertEquals(2, sut.replaySeries.size)
        assertEquals(inputSeries[1].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(candleState, sut.replaySeries.candleState.value)
    }

    @Test
    fun reset_closed() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = inputSeries,
            initialIndex = 1,
        )

        sut.advanceTo(inputSeries[2].openInstant, BarReplay.CandleState.Close)
        sut.reset()

        assertEquals(inputSeries[0], sut.replaySeries.last())
        assertEquals(1, sut.replaySeries.size)
        assertEquals(inputSeries[0].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun reset_not_closed() {

        val sut = SimpleReplaySeriesBuilder(
            inputSeries = inputSeries,
            initialIndex = 1,
        )

        sut.advanceTo(inputSeries[1].openInstant, BarReplay.CandleState.Open)
        sut.reset()

        assertEquals(inputSeries[0], sut.replaySeries.last())
        assertEquals(1, sut.replaySeries.size)
        assertEquals(inputSeries[0].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    private val inputSeries = MutableCandleSeries(
        initial = listOf(
            Candle(
                Instant.fromEpochSeconds(1702629600),
                "21341.5".toBigDecimal(),
                "21347.85".toBigDecimal(),
                "21330".toBigDecimal(),
                "21330.7".toBigDecimal(),
                BigDecimal.ZERO
            ),
            Candle(
                Instant.fromEpochSeconds(1702629900),
                "21331.9".toBigDecimal(),
                "21339.65".toBigDecimal(),
                "21328.15".toBigDecimal(),
                "21335.6".toBigDecimal(),
                BigDecimal.ZERO
            ),
            Candle(
                Instant.fromEpochSeconds(1702630200),
                "21336.4".toBigDecimal(),
                "21351.75".toBigDecimal(),
                "21333.15".toBigDecimal(),
                "21341.1".toBigDecimal(),
                BigDecimal.ZERO
            ),
            Candle(
                Instant.fromEpochSeconds(1702630500),
                "21342.1".toBigDecimal(),
                "21347.95".toBigDecimal(),
                "21336.05".toBigDecimal(),
                "21346.3".toBigDecimal(),
                BigDecimal.ZERO
            ),
            Candle(
                Instant.fromEpochSeconds(1702630800),
                "21346.4".toBigDecimal(),
                "21356.85".toBigDecimal(),
                "21345.25".toBigDecimal(),
                "21354.6".toBigDecimal(),
                BigDecimal.ZERO
            ),
            Candle(
                Instant.fromEpochSeconds(1702631100),
                "21354.2".toBigDecimal(),
                "21356".toBigDecimal(),
                "21344.6".toBigDecimal(),
                "21350.8".toBigDecimal(),
                BigDecimal.ZERO
            ),
        ),
        timeframe = Timeframe.M5,
    ).asCandleSeries()
}

package com.saurabhsandav.core.trading.barreplay

import com.saurabhsandav.core.trading.*
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ResampledReplaySeriesBuilderTest {

    @Test
    fun init() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = inputSeries,
            timeframeSeries = timeframeSeries,
            initialIndex = 6,
            currentOffset = 0,
            currentCandleState = BarReplay.CandleState.Close,
            sessionChecker = DailySessionChecker,
        )

        assertEquals(timeframeSeries[1], sut.replaySeries.last())
        assertEquals(2, sut.replaySeries.size)
        assertEquals(timeframeSeries[1].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun init_partial() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = inputSeries,
            timeframeSeries = timeframeSeries,
            initialIndex = 7,
            currentOffset = 0,
            currentCandleState = BarReplay.CandleState.Close,
            sessionChecker = DailySessionChecker,
        )

        assertEquals(inputSeries[6], sut.replaySeries.last())
        assertEquals(3, sut.replaySeries.size)
        assertEquals(inputSeries[6].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun addCandle() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = inputSeries,
            timeframeSeries = timeframeSeries,
            initialIndex = 6,
            currentOffset = 0,
            currentCandleState = BarReplay.CandleState.Close,
            sessionChecker = DailySessionChecker,
        )

        sut.addCandle(0)
        sut.addCandle(1)
        sut.addCandle(2)

        assertEquals(timeframeSeries[2], sut.replaySeries.last())
        assertEquals(3, sut.replaySeries.size)
        assertEquals(inputSeries[8].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun addCandle_not_closed() {

        val candleState = BarReplay.CandleState.Extreme2

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = inputSeries,
            timeframeSeries = timeframeSeries,
            initialIndex = 6,
            currentOffset = 0,
            currentCandleState = BarReplay.CandleState.Close,
            sessionChecker = DailySessionChecker,
        )

        sut.addCandle(0)
        sut.addCandle(1)
        sut.addCandle(2)
        sut.addCandle(3, BarReplay.CandleState.Open)
        sut.addCandle(3, BarReplay.CandleState.Extreme1)
        sut.addCandle(3, candleState)

        assertEquals(inputSeries[9].atState(candleState), sut.replaySeries.last())
        assertEquals(4, sut.replaySeries.size)
        assertEquals(inputSeries[9].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(candleState, sut.replaySeries.candleState.value)
    }

    @Test
    fun reset_closed() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = inputSeries,
            timeframeSeries = timeframeSeries,
            initialIndex = 6,
            currentOffset = 0,
            currentCandleState = BarReplay.CandleState.Close,
            sessionChecker = DailySessionChecker,
        )

        sut.addCandle(0)
        sut.addCandle(1)
        sut.reset()

        assertEquals(timeframeSeries[1], sut.replaySeries.last())
        assertEquals(2, sut.replaySeries.size)
        assertEquals(timeframeSeries[1].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    @Test
    fun reset_not_closed() {

        val sut = ResampledReplaySeriesBuilder(
            inputSeries = inputSeries,
            timeframeSeries = timeframeSeries,
            initialIndex = 6,
            currentOffset = 0,
            currentCandleState = BarReplay.CandleState.Close,
            sessionChecker = DailySessionChecker,
        )

        sut.addCandle(0, BarReplay.CandleState.Open)
        sut.reset()

        assertEquals(timeframeSeries[1], sut.replaySeries.last())
        assertEquals(2, sut.replaySeries.size)
        assertEquals(timeframeSeries[1].openInstant, sut.replaySeries.replayTime.value)
        assertEquals(BarReplay.CandleState.Close, sut.replaySeries.candleState.value)
    }

    private val inputSeries = MutableCandleSeries(
        initial = listOf(
            Candle(
                Instant.fromEpochSeconds(1702624500),
                "297.55".toBigDecimal(),
                "297.7".toBigDecimal(),
                "297.3".toBigDecimal(),
                "297.35".toBigDecimal(),
                "260304".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702624800),
                "297.35".toBigDecimal(),
                "297.5".toBigDecimal(),
                "297.3".toBigDecimal(),
                "297.4".toBigDecimal(),
                "32179".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702625100),
                "297.4".toBigDecimal(),
                "297.4".toBigDecimal(),
                "297.2".toBigDecimal(),
                "297.35".toBigDecimal(),
                "72328".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702625400),
                "297.3".toBigDecimal(),
                "297.45".toBigDecimal(),
                "297.25".toBigDecimal(),
                "297.35".toBigDecimal(),
                "28748".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702625700),
                "297.35".toBigDecimal(),
                "297.45".toBigDecimal(),
                "297.25".toBigDecimal(),
                "297.4".toBigDecimal(),
                "264255".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702626000),
                "297.4".toBigDecimal(),
                "297.7".toBigDecimal(),
                "297.35".toBigDecimal(),
                "297.5".toBigDecimal(),
                "158485".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702626300),
                "297.45".toBigDecimal(),
                "297.5".toBigDecimal(),
                "296.85".toBigDecimal(),
                "297".toBigDecimal(),
                "133020".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702626600),
                "296.9".toBigDecimal(),
                "297".toBigDecimal(),
                "296.3".toBigDecimal(),
                "296.45".toBigDecimal(),
                "385188".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702626900),
                "296.3".toBigDecimal(),
                "296.65".toBigDecimal(),
                "296.2".toBigDecimal(),
                "296.6".toBigDecimal(),
                "102969".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702627200),
                "296.65".toBigDecimal(),
                "297.1".toBigDecimal(),
                "296.55".toBigDecimal(),
                "297".toBigDecimal(),
                "97349".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702627500),
                "296.95".toBigDecimal(),
                "297.2".toBigDecimal(),
                "296.8".toBigDecimal(),
                "297.2".toBigDecimal(),
                "193037".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702627800),
                "297.2".toBigDecimal(),
                "297.8".toBigDecimal(),
                "297.2".toBigDecimal(),
                "297.6".toBigDecimal(),
                "296911".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702628100),
                "297.6".toBigDecimal(),
                "298".toBigDecimal(),
                "297.55".toBigDecimal(),
                "297.9".toBigDecimal(),
                "128112".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702628400),
                "297.8".toBigDecimal(),
                "298.15".toBigDecimal(),
                "297.75".toBigDecimal(),
                "297.9".toBigDecimal(),
                "194660".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702628700),
                "297.9".toBigDecimal(),
                "298.15".toBigDecimal(),
                "297.75".toBigDecimal(),
                "298.1".toBigDecimal(),
                "223943".toBigDecimal()
            ),
        ),
        timeframe = Timeframe.M5,
    ).asCandleSeries()

    private val timeframeSeries = MutableCandleSeries(
        initial = listOf(
            Candle(
                Instant.fromEpochSeconds(1702624500),
                "297.55".toBigDecimal(),
                "297.7".toBigDecimal(),
                "297.2".toBigDecimal(),
                "297.35".toBigDecimal(),
                "364811".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702625400),
                "297.3".toBigDecimal(),
                "297.7".toBigDecimal(),
                "297.25".toBigDecimal(),
                "297.5".toBigDecimal(),
                "451488".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702626300),
                "297.45".toBigDecimal(),
                "297.5".toBigDecimal(),
                "296.2".toBigDecimal(),
                "296.6".toBigDecimal(),
                "621177".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702627200),
                "296.65".toBigDecimal(),
                "297.8".toBigDecimal(),
                "296.55".toBigDecimal(),
                "297.6".toBigDecimal(),
                "587297".toBigDecimal()
            ),
            Candle(
                Instant.fromEpochSeconds(1702628100),
                "297.6".toBigDecimal(),
                "298.15".toBigDecimal(),
                "297.55".toBigDecimal(),
                "298.1".toBigDecimal(),
                "546715".toBigDecimal()
            ),
        ),
        timeframe = Timeframe.M15,
    )
}

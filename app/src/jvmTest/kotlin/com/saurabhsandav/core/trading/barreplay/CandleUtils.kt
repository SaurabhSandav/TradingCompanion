package com.saurabhsandav.core.trading.barreplay

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.saurabhsandav.core.trading.*
import kotlinx.datetime.Instant

object CandleUtils {

    val m5Series by getCandlesSeries(Timeframe.M5)

    val m15Series by getCandlesSeries(Timeframe.M15)

    val d1Series by getCandlesSeries(Timeframe.D1)

    fun resample(
        resampledCandle: Candle,
        newCandle: Candle,
    ): Candle = resampledCandle.copy(
        high = if (resampledCandle.high > newCandle.high) resampledCandle.high else newCandle.high,
        low = if (resampledCandle.low < newCandle.low) resampledCandle.low else newCandle.low,
        close = newCandle.close,
        volume = resampledCandle.volume + newCandle.volume,
    )

    private fun getCandlesSeries(timeframe: Timeframe): Lazy<CandleSeries> = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {

        require(timeframe in listOf(Timeframe.M5, Timeframe.M15, Timeframe.D1))

        this::class.java.getResourceAsStream("/NTPC_$timeframe.csv")
            .let(::requireNotNull)
            .use { csvReader().readAll(it) }
            .map { list ->
                Candle(
                    openInstant = Instant.fromEpochSeconds(list[0].toLong()),
                    open = list[1].toBigDecimal(),
                    high = list[2].toBigDecimal(),
                    low = list[3].toBigDecimal(),
                    close = list[4].toBigDecimal(),
                    volume = list[5].toBigDecimal(),
                )
            }
            .let { candles -> MutableCandleSeries(candles, timeframe = timeframe).asCandleSeries() }
    }
}

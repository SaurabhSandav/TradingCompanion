package com.saurabhsandav.core.trading.barreplay

import app.softwork.serialization.csv.CSVFormat
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.asCandleSeries
import kotlinx.serialization.decodeFromString

object CandleUtils {

    private val csvFormat = CSVFormat { includeHeader = false }

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
            .let { inputStream ->
                val csvText = inputStream.reader().use { it.readText() }
                val candles = csvFormat.decodeFromString<List<Candle>>(csvText)
                MutableCandleSeries(candles, timeframe = timeframe).asCandleSeries()
            }
    }
}

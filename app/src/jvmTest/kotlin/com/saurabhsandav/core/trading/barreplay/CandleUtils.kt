package com.saurabhsandav.core.trading.barreplay

import app.softwork.serialization.csv.CSVFormat
import com.saurabhsandav.core.trading.core.Candle
import com.saurabhsandav.core.trading.core.CandleSeries
import com.saurabhsandav.core.trading.core.MutableCandleSeries
import com.saurabhsandav.core.trading.core.Timeframe
import com.saurabhsandav.core.trading.core.asCandleSeries
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import java.math.BigDecimal
import kotlin.time.Instant

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
                val candles = csvFormat.decodeFromString(ListSerializer(CandleSerializer), csvText)
                MutableCandleSeries(candles, timeframe = timeframe).asCandleSeries()
            }
    }
}

private object CandleSerializer : KSerializer<Candle> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Candle") {
        element<Long>("openInstant")
        element<String>("open")
        element<String>("high")
        element<String>("low")
        element<String>("close")
        element<String>("volume")
    }

    override fun serialize(
        encoder: Encoder,
        value: Candle,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.openInstant.epochSeconds)
            encodeStringElement(descriptor, 1, value.open.toPlainString())
            encodeStringElement(descriptor, 2, value.high.toPlainString())
            encodeStringElement(descriptor, 3, value.low.toPlainString())
            encodeStringElement(descriptor, 4, value.close.toPlainString())
            encodeStringElement(descriptor, 5, value.volume.toPlainString())
        }
    }

    override fun deserialize(decoder: Decoder): Candle = decoder.decodeStructure(descriptor) {

        var openInstant: Instant? = null
        var open: BigDecimal? = null
        var high: BigDecimal? = null
        var low: BigDecimal? = null
        var close: BigDecimal? = null
        var volume: BigDecimal? = null

        if (decodeSequentially()) {
            openInstant = decodeLongElement(descriptor, 0).let(Instant::fromEpochSeconds)
            open = decodeStringElement(descriptor, 1).let(::BigDecimal)
            high = decodeStringElement(descriptor, 2).let(::BigDecimal)
            low = decodeStringElement(descriptor, 3).let(::BigDecimal)
            close = decodeStringElement(descriptor, 4).let(::BigDecimal)
            volume = decodeStringElement(descriptor, 5).let(::BigDecimal)
        } else {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> openInstant = decodeLongElement(descriptor, 0).let(Instant::fromEpochSeconds)
                    1 -> open = decodeStringElement(descriptor, 1).let(::BigDecimal)
                    2 -> high = decodeStringElement(descriptor, 2).let(::BigDecimal)
                    3 -> low = decodeStringElement(descriptor, 3).let(::BigDecimal)
                    4 -> close = decodeStringElement(descriptor, 4).let(::BigDecimal)
                    5 -> volume = decodeStringElement(descriptor, 5).let(::BigDecimal)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unknown index $index")
                }
            }
        }

        Candle(
            openInstant = requireNotNull(openInstant) { "Missing openInstant" },
            open = requireNotNull(open) { "Missing open" },
            high = requireNotNull(high) { "Missing high" },
            low = requireNotNull(low) { "Missing low" },
            close = requireNotNull(close) { "Missing close" },
            volume = requireNotNull(volume) { "Missing volume" },
        )
    }
}

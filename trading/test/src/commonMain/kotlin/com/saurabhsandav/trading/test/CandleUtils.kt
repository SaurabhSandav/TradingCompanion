package com.saurabhsandav.trading.test

import app.softwork.serialization.csv.CSVFormat
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.core.Candle
import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.MutableCandleSeries
import com.saurabhsandav.trading.core.Timeframe
import com.saurabhsandav.trading.core.asCandleSeries
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
            encodeStringElement(descriptor, 1, value.open.toString())
            encodeStringElement(descriptor, 2, value.high.toString())
            encodeStringElement(descriptor, 3, value.low.toString())
            encodeStringElement(descriptor, 4, value.close.toString())
            encodeStringElement(descriptor, 5, value.volume.toString())
        }
    }

    override fun deserialize(decoder: Decoder): Candle = decoder.decodeStructure(descriptor) {

        var openInstant: Instant? = null
        var open: KBigDecimal? = null
        var high: KBigDecimal? = null
        var low: KBigDecimal? = null
        var close: KBigDecimal? = null
        var volume: KBigDecimal? = null

        if (decodeSequentially()) {
            openInstant = decodeLongElement(descriptor, 0).let(Instant::fromEpochSeconds)
            open = decodeStringElement(descriptor, 1).let(::KBigDecimal)
            high = decodeStringElement(descriptor, 2).let(::KBigDecimal)
            low = decodeStringElement(descriptor, 3).let(::KBigDecimal)
            close = decodeStringElement(descriptor, 4).let(::KBigDecimal)
            volume = decodeStringElement(descriptor, 5).let(::KBigDecimal)
        } else {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> openInstant = decodeLongElement(descriptor, 0).let(Instant::fromEpochSeconds)
                    1 -> open = decodeStringElement(descriptor, 1).let(::KBigDecimal)
                    2 -> high = decodeStringElement(descriptor, 2).let(::KBigDecimal)
                    3 -> low = decodeStringElement(descriptor, 3).let(::KBigDecimal)
                    4 -> close = decodeStringElement(descriptor, 4).let(::KBigDecimal)
                    5 -> volume = decodeStringElement(descriptor, 5).let(::KBigDecimal)
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

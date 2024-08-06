package com.saurabhsandav.lightweight_charts.options.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = PriceLineSource.Serializer::class)
enum class PriceLineSource(private val intValue: Int) {

    LastBar(0),
    LastVisible(1);

    internal object Serializer : KSerializer<PriceLineSource> {

        override val descriptor = PrimitiveSerialDescriptor("PriceLineSourceSerializer", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): PriceLineSource {
            val intValue = decoder.decodeInt()
            return PriceLineSource.entries.find { it.intValue == intValue } ?: error("Invalid PriceLineSource")
        }

        override fun serialize(encoder: Encoder, value: PriceLineSource) {
            encoder.encodeInt(value.intValue)
        }
    }
}

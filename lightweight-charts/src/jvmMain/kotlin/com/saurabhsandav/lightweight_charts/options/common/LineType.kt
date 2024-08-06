package com.saurabhsandav.lightweight_charts.options.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = LineType.Serializer::class)
enum class LineType(private val intValue: Int) {

    Simple(0),
    WithSteps(1),
    Curved(2);

    internal object Serializer : KSerializer<LineType> {

        override val descriptor = PrimitiveSerialDescriptor("LineTypeSerializer", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): LineType {
            val intValue = decoder.decodeInt()
            return LineType.entries.find { it.intValue == intValue } ?: error("Invalid LineType")
        }

        override fun serialize(encoder: Encoder, value: LineType) {
            encoder.encodeInt(value.intValue)
        }
    }
}

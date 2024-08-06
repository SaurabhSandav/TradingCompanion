package com.saurabhsandav.lightweight_charts.options.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = LineStyle.Serializer::class)
enum class LineStyle(private val intValue: Int) {

    Solid(0),
    Dotted(1),
    Dashed(2),
    LargeDashed(3),
    SparseDotted(4);

    internal object Serializer : KSerializer<LineStyle> {

        override val descriptor = PrimitiveSerialDescriptor("LineStyleSerializer", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): LineStyle {
            val intValue = decoder.decodeInt()
            return LineStyle.entries.find { it.intValue == intValue } ?: error("Invalid LineStyle")
        }

        override fun serialize(encoder: Encoder, value: LineStyle) {
            encoder.encodeInt(value.intValue)
        }
    }
}

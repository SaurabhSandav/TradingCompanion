package com.saurabhsandav.lightweight_charts.options.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = LineWidth.Serializer::class)
enum class LineWidth(private val intValue: Int) {

    One(1),
    Two(2),
    Three(3),
    Four(4);

    internal object Serializer : KSerializer<LineWidth> {

        override val descriptor = PrimitiveSerialDescriptor("LineWidthSerializer", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): LineWidth {
            val intValue = decoder.decodeInt()
            return LineWidth.entries.find { it.intValue == intValue } ?: error("Invalid LineWidth")
        }

        override fun serialize(encoder: Encoder, value: LineWidth) {
            encoder.encodeInt(value.intValue)
        }
    }
}

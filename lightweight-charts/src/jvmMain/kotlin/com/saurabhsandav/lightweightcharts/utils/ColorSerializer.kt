package com.saurabhsandav.lightweightcharts.utils

import kotlinx.css.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias SerializableColor =
    @Serializable(ColorSerializer::class)
    Color

internal object ColorSerializer : KSerializer<Color> {

    override val descriptor = PrimitiveSerialDescriptor("ColorSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Color {
        return Color(decoder.decodeString())
    }

    override fun serialize(
        encoder: Encoder,
        value: Color,
    ) {
        encoder.encodeString(value.value)
    }
}

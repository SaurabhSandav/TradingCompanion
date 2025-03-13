package com.saurabhsandav.lightweightcharts.options.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = LastPriceAnimationMode.Serializer::class)
enum class LastPriceAnimationMode(
    private val intValue: Int,
) {

    Disabled(0),
    Continuous(1),
    OnDataUpdate(2),
    ;

    internal object Serializer : KSerializer<LastPriceAnimationMode> {

        override val descriptor = PrimitiveSerialDescriptor("LastPriceAnimationModeSerializer", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): LastPriceAnimationMode {

            val intValue = decoder.decodeInt()

            return LastPriceAnimationMode
                .entries
                .find { it.intValue == intValue }
                ?: error("Invalid LastPriceAnimationMode")
        }

        override fun serialize(
            encoder: Encoder,
            value: LastPriceAnimationMode,
        ) {
            encoder.encodeInt(value.intValue)
        }
    }
}

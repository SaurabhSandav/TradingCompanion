package com.saurabhsandav.core.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import kotlin.time.Instant

object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor = PrimitiveSerialDescriptor("BigDecimalSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        return decoder.decodeString().toBigDecimal()
    }

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal,
    ) {
        encoder.encodeString(value.toPlainString())
    }
}

object InstantEpochSecondsSerializer : KSerializer<Instant> {

    override val descriptor = PrimitiveSerialDescriptor("InstantEpochSecondsSerializer", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.fromEpochSeconds(decoder.decodeLong())
    }

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        encoder.encodeLong(value.epochSeconds)
    }
}

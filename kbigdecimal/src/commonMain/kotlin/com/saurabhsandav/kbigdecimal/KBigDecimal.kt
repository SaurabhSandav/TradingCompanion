package com.saurabhsandav.kbigdecimal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public expect class BigDecimalType

@Serializable(with = KBigDecimalSerializer::class)
public expect value class KBigDecimal(
    private val value: BigDecimalType,
) : Comparable<KBigDecimal> {

    public constructor(value: String)

    public operator fun plus(other: KBigDecimal): KBigDecimal

    public fun plus(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal

    public operator fun minus(other: KBigDecimal): KBigDecimal

    public fun minus(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal

    public operator fun times(other: KBigDecimal): KBigDecimal

    public fun times(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal

    public operator fun div(other: KBigDecimal): KBigDecimal

    public fun div(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal

    public fun div(
        other: KBigDecimal,
        roundingMode: KRoundingMode,
    ): KBigDecimal

    public fun div(
        other: KBigDecimal,
        decimalPlaces: Int,
        roundingMode: KRoundingMode,
    ): KBigDecimal

    public fun remainder(other: KBigDecimal): KBigDecimal

    public fun abs(): KBigDecimal

    public fun negated(): KBigDecimal

    public fun decimalPlaces(
        decimalPlaces: Int,
        roundingMode: KRoundingMode,
    ): KBigDecimal

    override operator fun compareTo(other: KBigDecimal): Int

    public companion object {

        public val config: Config

        public val Zero: KBigDecimal
        public val One: KBigDecimal
    }
}

public interface Config {

    public var decimalPlaces: Int

    public var roundingMode: KRoundingMode
}

public object KBigDecimalSerializer : KSerializer<KBigDecimal> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("KBigDecimalSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): KBigDecimal {
        return decoder.decodeString().toKBigDecimal()
    }

    override fun serialize(
        encoder: Encoder,
        value: KBigDecimal,
    ) {
        encoder.encodeString(value.toString())
    }
}

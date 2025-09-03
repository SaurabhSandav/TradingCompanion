package com.saurabhsandav.kbigdecimal

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

public actual typealias BigDecimalType = BigDecimal

@Serializable(with = KBigDecimalSerializer::class)
@JvmInline
public actual value class KBigDecimal(
    internal val value: BigDecimal,
) : Comparable<KBigDecimal> {

    public actual constructor(value: String) : this(BigDecimal(value))

    public actual operator fun plus(other: KBigDecimal): KBigDecimal {
        return value.add(other.value).stripTrailingZeros().toKBigDecimal()
    }

    public actual fun plus(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal {
        return value.add(other.value, kMathContext.toMathContext()).stripTrailingZeros().toKBigDecimal()
    }

    public actual operator fun minus(other: KBigDecimal): KBigDecimal {
        return value.subtract(other.value).stripTrailingZeros().toKBigDecimal()
    }

    public actual fun minus(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal {
        return value.subtract(other.value, kMathContext.toMathContext()).stripTrailingZeros().toKBigDecimal()
    }

    public actual operator fun times(other: KBigDecimal): KBigDecimal {
        return value.multiply(other.value).stripTrailingZeros().toKBigDecimal()
    }

    public actual fun times(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal {
        return value.multiply(other.value, kMathContext.toMathContext()).stripTrailingZeros().toKBigDecimal()
    }

    public actual operator fun div(other: KBigDecimal): KBigDecimal {
        return value.divide(other.value, config.decimalPlaces, config.roundingMode.toPlatformRM())
            .stripTrailingZeros()
            .toKBigDecimal()
    }

    public actual fun div(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal {
        return value.divide(other.value, kMathContext.toMathContext()).stripTrailingZeros().toKBigDecimal()
    }

    public actual fun div(
        other: KBigDecimal,
        roundingMode: KRoundingMode,
    ): KBigDecimal {
        return value.divide(other.value, config.decimalPlaces, roundingMode.toPlatformRM())
            .stripTrailingZeros()
            .toKBigDecimal()
    }

    public actual fun div(
        other: KBigDecimal,
        decimalPlaces: Int,
        roundingMode: KRoundingMode,
    ): KBigDecimal {
        return value.divide(other.value, decimalPlaces, roundingMode.toPlatformRM())
            .stripTrailingZeros()
            .toKBigDecimal()
    }

    public actual fun remainder(other: KBigDecimal): KBigDecimal {
        return value.remainder(other.value).stripTrailingZeros().toKBigDecimal()
    }

    public actual fun abs(): KBigDecimal {
        return value.abs().toKBigDecimal()
    }

    public actual fun negated(): KBigDecimal {
        return value.negate().toKBigDecimal()
    }

    public actual fun decimalPlaces(
        decimalPlaces: Int,
        roundingMode: KRoundingMode,
    ): KBigDecimal {
        return value.setScale(decimalPlaces, roundingMode.toPlatformRM()).stripTrailingZeros().toKBigDecimal()
    }

    actual override operator fun compareTo(other: KBigDecimal): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return value.toPlainString()
    }

    public actual companion object {

        public actual val config: Config = object : Config {

            override var decimalPlaces: Int = 20

            override var roundingMode: KRoundingMode = KRoundingMode.HalfUp
        }

        public actual val Zero: KBigDecimal = BigDecimal.ZERO.toKBigDecimal()
        public actual val One: KBigDecimal = BigDecimal.ONE.toKBigDecimal()
    }
}

private fun BigDecimal.toKBigDecimal(): KBigDecimal = KBigDecimal(this)

private fun KMathContext.toMathContext(): MathContext = when (roundingMode) {
    null -> MathContext(precision)
    else -> MathContext(precision, roundingMode.toPlatformRM())
}

public fun KRoundingMode.toPlatformRM(): RoundingMode = when (this) {
    KRoundingMode.Up -> RoundingMode.UP
    KRoundingMode.Down -> RoundingMode.DOWN
    KRoundingMode.Ceiling -> RoundingMode.CEILING
    KRoundingMode.Floor -> RoundingMode.FLOOR
    KRoundingMode.HalfUp -> RoundingMode.HALF_UP
    KRoundingMode.HalfDown -> RoundingMode.HALF_DOWN
    KRoundingMode.HalfEven -> RoundingMode.HALF_EVEN
}

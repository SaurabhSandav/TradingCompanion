package com.saurabhsandav.kbigdecimal

import kotlinx.serialization.Serializable

public actual typealias BigDecimalType = BigNumber

@Serializable(with = KBigDecimalSerializer::class)
public actual value class KBigDecimal(
    internal val value: BigNumber,
) : Comparable<KBigDecimal> {

    public actual constructor(value: String) : this(BigNumber(value))

    init {
        if (value.isNaN()) throw NumberFormatException("Value is NaN")
        if (!value.isFinite()) throw NumberFormatException("Value cannot be Infinite")
    }

    public actual operator fun plus(other: KBigDecimal): KBigDecimal {
        return value.plus(other.value).toKBigDecimal()
    }

    public actual fun plus(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal {
        return value.plus(other.value).toKBigDecimal(kMathContext)
    }

    public actual operator fun minus(other: KBigDecimal): KBigDecimal {
        return value.minus(other.value).toKBigDecimal()
    }

    public actual fun minus(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal {
        return value.minus(other.value).toKBigDecimal(kMathContext)
    }

    public actual operator fun times(other: KBigDecimal): KBigDecimal {
        return value.times(other.value).toKBigDecimal()
    }

    public actual fun times(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal {
        return value.times(other.value).toKBigDecimal(kMathContext)
    }

    public actual operator fun div(other: KBigDecimal): KBigDecimal {

        val config = BigNumberConfig {
            DECIMAL_PLACES = config.decimalPlaces
            ROUNDING_MODE = config.roundingMode.toPlatformRM()
        }

        return BigNumber.withCustomConfig(config) { value.div(other.value).toKBigDecimal() }
    }

    public actual fun div(
        other: KBigDecimal,
        kMathContext: KMathContext,
    ): KBigDecimal {
        return value.div(other.value).toKBigDecimal(kMathContext)
    }

    public actual fun div(
        other: KBigDecimal,
        roundingMode: KRoundingMode,
    ): KBigDecimal {

        val config = BigNumberConfig {
            ROUNDING_MODE = roundingMode.toPlatformRM()
        }

        return BigNumber.withCustomConfig(config) { value.div(other.value).toKBigDecimal() }
    }

    public actual fun div(
        other: KBigDecimal,
        decimalPlaces: Int,
        roundingMode: KRoundingMode,
    ): KBigDecimal {

        val config = BigNumberConfig {
            DECIMAL_PLACES = decimalPlaces
            ROUNDING_MODE = roundingMode.toPlatformRM()
        }

        return BigNumber.withCustomConfig(config) { value.div(other.value).toKBigDecimal() }
    }

    public actual fun remainder(other: KBigDecimal): KBigDecimal {
        return value.modulo(other.value).toKBigDecimal()
    }

    public actual fun abs(): KBigDecimal {
        return value.abs().toKBigDecimal()
    }

    public actual fun negated(): KBigDecimal {
        return value.negated().toKBigDecimal()
    }

    public actual fun decimalPlaces(
        decimalPlaces: Int,
        roundingMode: KRoundingMode,
    ): KBigDecimal {
        return value.decimalPlaces(decimalPlaces, roundingMode.toPlatformRM()).toKBigDecimal()
    }

    public actual override operator fun compareTo(other: KBigDecimal): Int {
        return value.comparedTo(other.value)!!
    }

    override fun toString(): String {
        return value.toFixed()
    }

    public actual companion object {

        public actual val config: Config = object : Config {

            override var decimalPlaces: Int
                get() = BigNumber.config().DECIMAL_PLACES
                set(value) {
                    BigNumber.config(BigNumberConfig { DECIMAL_PLACES = value })
                }

            override var roundingMode: KRoundingMode
                get() = BigNumber.config().ROUNDING_MODE.toKRoundingMode()
                set(value) {
                    BigNumber.config(BigNumberConfig { ROUNDING_MODE = value.toPlatformRM() })
                }
        }

        public actual val Zero: KBigDecimal = KBigDecimal("0")
        public actual val One: KBigDecimal = KBigDecimal("1")
    }
}

private fun BigNumber.toKBigDecimal(kMathContext: KMathContext? = null): KBigDecimal {

    if (isNaN()) throw ArithmeticException("Result is NaN")

    val result = when (kMathContext) {
        null -> this
        else -> precision(
            precision = kMathContext.precision,
            roundingMode = kMathContext.roundingMode?.toPlatformRM(),
        )
    }

    return KBigDecimal(result)
}

public fun KRoundingMode.toPlatformRM(): Int = when (this) {
    KRoundingMode.Up -> BigNumber.ROUND_UP
    KRoundingMode.Down -> BigNumber.ROUND_DOWN
    KRoundingMode.Ceiling -> BigNumber.ROUND_CEIL
    KRoundingMode.Floor -> BigNumber.ROUND_FLOOR
    KRoundingMode.HalfUp -> BigNumber.ROUND_HALF_UP
    KRoundingMode.HalfDown -> BigNumber.ROUND_HALF_DOWN
    KRoundingMode.HalfEven -> BigNumber.ROUND_HALF_EVEN
}

private fun Int.toKRoundingMode(): KRoundingMode = when (this) {
    BigNumber.ROUND_UP -> KRoundingMode.Up
    BigNumber.ROUND_DOWN -> KRoundingMode.Down
    BigNumber.ROUND_CEIL -> KRoundingMode.Ceiling
    BigNumber.ROUND_FLOOR -> KRoundingMode.Floor
    BigNumber.ROUND_HALF_UP -> KRoundingMode.HalfUp
    BigNumber.ROUND_HALF_DOWN -> KRoundingMode.HalfDown
    BigNumber.ROUND_HALF_EVEN -> KRoundingMode.HalfEven
    else -> error("Unsupported Rounding Mode: $this")
}

package com.saurabhsandav.kbigdecimal

import kotlin.js.JsModule
import kotlin.js.JsNumber
import kotlin.js.definedExternally
import kotlin.js.js

@JsModule("bignumber.js")
public external class BigNumber(
    value: String,
) {

    public fun plus(other: BigNumber): BigNumber

    public fun minus(other: BigNumber): BigNumber

    public fun times(other: BigNumber): BigNumber

    public fun div(other: BigNumber): BigNumber

    public fun integerValue(roundingMode: Int = definedExternally): Int

    public fun modulo(other: BigNumber): BigNumber

    public fun negated(): BigNumber

    public fun abs(): BigNumber

    public fun isNaN(): Boolean

    public fun isFinite(): Boolean

    public fun isZero(): Boolean

    public fun comparedTo(other: BigNumber): Int?

    public fun isEqualTo(other: BigNumber): Boolean

    public fun toFixed(): String

    public fun toNumber(): JsNumber

    public fun precision(): Int?

    public fun precision(
        precision: Int,
        roundingMode: Int? = definedExternally,
    ): BigNumber

    public fun decimalPlaces(): Int?

    public fun decimalPlaces(
        decimalPlaces: Int,
        roundingMode: Int? = definedExternally,
    ): BigNumber

    public companion object {

        public val ROUND_UP: Int
        public val ROUND_DOWN: Int
        public val ROUND_CEIL: Int
        public val ROUND_FLOOR: Int
        public val ROUND_HALF_UP: Int
        public val ROUND_HALF_DOWN: Int
        public val ROUND_HALF_EVEN: Int
        public val ROUND_HALF_CEIL: Int
        public val ROUND_HALF_FLOOR: Int

        public fun config(config: BigNumberConfig = definedExternally): BigNumberConfig
    }
}

@Suppress("PropertyName")
public external interface BigNumberConfig {

    public var DECIMAL_PLACES: Int

    public var ROUNDING_MODE: Int
}

private fun BigNumberConfig(): BigNumberConfig = js("({})")

public fun BigNumberConfig(block: BigNumberConfig.() -> Unit): BigNumberConfig = BigNumberConfig().apply(block)

internal fun <T> BigNumber.Companion.withCustomConfig(
    config: BigNumberConfig,
    block: () -> T,
): T {

    val globalConfig = BigNumber.config()

    BigNumber.config(config)

    val result = block()

    BigNumber.config(globalConfig)

    return result
}

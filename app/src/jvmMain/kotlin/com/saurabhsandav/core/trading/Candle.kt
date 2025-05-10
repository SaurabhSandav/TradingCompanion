package com.saurabhsandav.core.trading

import com.saurabhsandav.core.utils.BigDecimalSerializer
import com.saurabhsandav.core.utils.InstantEpochSecondsSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Suppress("ktlint:standard:no-blank-line-in-list")
@Serializable
data class Candle(

    @Serializable(with = InstantEpochSecondsSerializer::class)
    val openInstant: Instant,

    @Serializable(with = BigDecimalSerializer::class)
    val open: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    val high: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    val low: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    val close: BigDecimal,

    @Serializable(with = BigDecimalSerializer::class)
    val volume: BigDecimal,
)

val Candle.isLong: Boolean
    get() = close > open

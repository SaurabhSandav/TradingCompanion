package com.saurabhsandav.trading.core

import java.math.BigDecimal
import kotlin.time.Instant

data class Candle(
    val openInstant: Instant,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal,
)

val Candle.isLong: Boolean
    get() = close > open

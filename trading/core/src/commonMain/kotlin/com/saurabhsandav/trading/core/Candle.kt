package com.saurabhsandav.trading.core

import com.saurabhsandav.kbigdecimal.KBigDecimal
import kotlin.time.Instant

data class Candle(
    val openInstant: Instant,
    val open: KBigDecimal,
    val high: KBigDecimal,
    val low: KBigDecimal,
    val close: KBigDecimal,
    val volume: KBigDecimal,
)

val Candle.isLong: Boolean
    get() = close > open

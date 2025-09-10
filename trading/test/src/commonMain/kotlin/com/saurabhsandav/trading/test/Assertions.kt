@file:Suppress("NOTHING_TO_INLINE")

package com.saurabhsandav.trading.test

import com.saurabhsandav.trading.core.Candle
import kotlin.test.assertEquals

inline fun assertCandleEquals(
    expected: Candle,
    actual: Candle,
    message: String? = null,
) {

    assertEquals(expected.openInstant, actual.openInstant, message)
    assertBDEquals(expected.open, actual.open, message)
    assertBDEquals(expected.high, actual.high, message)
    assertBDEquals(expected.low, actual.low, message)
    assertBDEquals(expected.close, actual.close, message)
    assertBDEquals(expected.volume, actual.volume, message)
}

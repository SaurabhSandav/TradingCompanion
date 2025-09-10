@file:Suppress("NOTHING_TO_INLINE")

package com.saurabhsandav.trading.test

import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

inline fun assertBDEquals(
    expected: BigDecimal?,
    actual: BigDecimal?,
    message: String? = null,
) {

    if (expected == null && actual == null) return
    if (expected == null && actual != null) fail(messagePrefix(message) + "expected:<null> but was:<$actual>")
    if (expected != null && actual == null) fail(messagePrefix(message) + "expected:<$expected> but was:<null>")

    assertEquals(
        expected = expected!!.compareTo(actual),
        actual = 0,
        message = messagePrefix(message) + "expected:<$expected> but was:<$actual>",
    )
}

inline fun assertBDEquals(
    expected: Int,
    actual: BigDecimal?,
) {

    assertNotNull(actual)

    assertBDEquals(expected.toBigDecimal(), actual)
}

inline fun assertBDEquals(
    expected: String,
    actual: BigDecimal?,
) {

    assertNotNull(actual)

    assertBDEquals(expected.toBigDecimal(), actual)
}

inline fun messagePrefix(message: String?) = if (message == null) "" else "$message. "

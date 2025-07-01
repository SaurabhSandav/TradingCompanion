package com.saurabhsandav.trading.test

import java.math.BigDecimal
import kotlin.test.assertNotNull
import kotlin.test.asserter

fun assertBDEquals(
    expected: BigDecimal,
    actual: BigDecimal,
    message: String? = null,
) {

    asserter.assertTrue(
        actual = expected.compareTo(actual) == 0,
        message = messagePrefix(message) + "expected:<$expected> but was:<$actual>",
    )
}

fun assertBDEquals(
    expected: Int,
    actual: BigDecimal?,
) {

    assertNotNull(actual)

    assertBDEquals(expected.toBigDecimal(), actual)
}

fun assertBDEquals(
    expected: String,
    actual: BigDecimal?,
) {

    assertNotNull(actual)

    assertBDEquals(expected.toBigDecimal(), actual)
}

private fun messagePrefix(message: String?) = if (message == null) "" else "$message. "

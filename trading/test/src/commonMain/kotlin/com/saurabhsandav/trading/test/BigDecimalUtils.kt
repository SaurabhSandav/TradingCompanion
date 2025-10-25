@file:Suppress("NOTHING_TO_INLINE")

package com.saurabhsandav.trading.test

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import kotlin.test.assertNotNull
import kotlin.test.asserter
import kotlin.test.fail

inline fun assertBDEquals(
    expected: KBigDecimal?,
    actual: KBigDecimal?,
    message: String? = null,
) {

    if (expected == null && actual == null) return
    if (expected == null && actual != null) fail(messagePrefix(message) + "expected:<null> but was:<$actual>")
    if (expected != null && actual == null) fail(messagePrefix(message) + "expected:<$expected> but was:<null>")

    asserter.assertTrue(
        lazyMessage = { messagePrefix(message) + "expected:<$expected> but was:<$actual>" },
        actual = expected!!.compareTo(actual!!) == 0,
    )
}

inline fun assertBDEquals(
    expected: Int,
    actual: KBigDecimal?,
) {

    assertNotNull(actual)

    assertBDEquals(expected.toKBigDecimal(), actual)
}

inline fun assertBDEquals(
    expected: String,
    actual: KBigDecimal?,
) {

    assertNotNull(actual)

    assertBDEquals(expected.toKBigDecimal(), actual)
}

inline fun messagePrefix(message: String?) = if (message == null) "" else "$message. "

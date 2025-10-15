@file:Suppress("NOTHING_TO_INLINE")

package com.saurabhsandav.trading.record

import com.saurabhsandav.trading.test.assertBDEquals
import kotlin.test.assertEquals

inline fun assertTradeEquals(
    expected: Trade,
    actual: Trade,
    ignoreId: Boolean = true,
    message: String? = null,
) {

    if (!ignoreId) assertEquals(expected.id, actual.id, message)

    assertEquals(expected.brokerId, actual.brokerId, message)
    assertEquals(expected.symbolId, actual.symbolId, message)
    assertEquals(expected.instrument, actual.instrument, message)
    assertBDEquals(expected.quantity, actual.quantity, message)
    assertBDEquals(expected.closedQuantity, actual.closedQuantity, message)
    assertEquals(expected.lots, actual.lots, message)
    assertEquals(expected.closedLots, actual.closedLots, message)
    assertEquals(expected.side, actual.side, message)
    assertBDEquals(expected.averageEntry, actual.averageEntry, message)
    assertEquals(expected.entryTimestamp, actual.entryTimestamp, message)
    assertBDEquals(expected.averageExit, actual.averageExit, message)
    assertEquals(expected.exitTimestamp, actual.exitTimestamp, message)
    assertBDEquals(expected.pnl, actual.pnl, message)
    assertBDEquals(expected.fees, actual.fees, message)
    assertBDEquals(expected.netPnl, actual.netPnl, message)
    assertEquals(expected.isClosed, actual.isClosed, message)
}

inline fun assertTradeListEquals(
    expected: List<Trade>,
    actual: List<Trade>,
    ignoreId: Boolean = true,
    message: String? = null,
) {

    assertEquals(expected.size, actual.size, "Trade lists are different sizes")

    expected.zip(actual).forEach { (expected, actual) ->

        assertTradeEquals(
            expected = expected,
            actual = actual,
            ignoreId = ignoreId,
            message = message,
        )
    }
}

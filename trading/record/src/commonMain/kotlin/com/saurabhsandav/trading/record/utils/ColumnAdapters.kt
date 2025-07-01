package com.saurabhsandav.trading.record.utils

import app.cash.sqldelight.ColumnAdapter
import java.math.BigDecimal
import kotlin.time.Instant

internal object BigDecimalColumnAdapter : ColumnAdapter<BigDecimal, String> {
    override fun decode(databaseValue: String): BigDecimal = databaseValue.toBigDecimal()

    override fun encode(value: BigDecimal): String = value.toPlainString()
}

internal object InstantReadableColumnAdapter : ColumnAdapter<Instant, String> {
    override fun decode(databaseValue: String): Instant = Instant.parse(databaseValue)

    override fun encode(value: Instant): String = value.toString()
}

package com.saurabhsandav.core.utils

import app.cash.sqldelight.ColumnAdapter
import java.math.BigDecimal
import kotlin.time.Instant

object InstantLongColumnAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant = Instant.fromEpochSeconds(databaseValue)

    override fun encode(value: Instant): Long = value.epochSeconds
}

object InstantColumnAdapter : ColumnAdapter<Instant, String> {
    override fun decode(databaseValue: String): Instant = Instant.parse(databaseValue)

    override fun encode(value: Instant): String = value.toString()
}

object BigDecimalColumnAdapter : ColumnAdapter<BigDecimal, String> {
    override fun decode(databaseValue: String): BigDecimal = databaseValue.toBigDecimal()

    override fun encode(value: BigDecimal): String = value.toPlainString()
}

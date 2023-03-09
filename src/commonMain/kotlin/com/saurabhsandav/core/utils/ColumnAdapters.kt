package com.saurabhsandav.core.utils

import com.squareup.sqldelight.ColumnAdapter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

object IntColumnAdapter : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long): Int = databaseValue.toInt()

    override fun encode(value: Int): Long = value.toLong()
}

object BigDecimalColumnAdapter : ColumnAdapter<BigDecimal, String> {
    override fun decode(databaseValue: String): BigDecimal = databaseValue.toBigDecimal()

    override fun encode(value: BigDecimal): String = value.toPlainString()
}

object LocalDateTimeColumnAdapter : ColumnAdapter<LocalDateTime, String> {
    override fun decode(databaseValue: String): LocalDateTime = LocalDateTime.parse(databaseValue)

    override fun encode(value: LocalDateTime): String = value.toString()
}

object InstantColumnAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant = Instant.fromEpochSeconds(databaseValue)

    override fun encode(value: Instant): Long = value.epochSeconds
}

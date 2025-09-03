package com.saurabhsandav.core.utils

import app.cash.sqldelight.ColumnAdapter
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import kotlin.time.Instant

object InstantLongColumnAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant = Instant.fromEpochSeconds(databaseValue)

    override fun encode(value: Instant): Long = value.epochSeconds
}

object InstantColumnAdapter : ColumnAdapter<Instant, String> {
    override fun decode(databaseValue: String): Instant = Instant.parse(databaseValue)

    override fun encode(value: Instant): String = value.toString()
}

object KBigDecimalColumnAdapter : ColumnAdapter<KBigDecimal, String> {
    override fun decode(databaseValue: String): KBigDecimal = databaseValue.toKBigDecimal()

    override fun encode(value: KBigDecimal): String = value.toString()
}

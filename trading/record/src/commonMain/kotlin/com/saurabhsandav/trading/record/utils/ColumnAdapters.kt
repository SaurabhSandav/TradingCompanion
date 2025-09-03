package com.saurabhsandav.trading.record.utils

import app.cash.sqldelight.ColumnAdapter
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimal
import kotlin.time.Instant

internal object KBigDecimalColumnAdapter : ColumnAdapter<KBigDecimal, String> {
    override fun decode(databaseValue: String): KBigDecimal = databaseValue.toKBigDecimal()

    override fun encode(value: KBigDecimal): String = value.toString()
}

internal object InstantReadableColumnAdapter : ColumnAdapter<Instant, String> {
    override fun decode(databaseValue: String): Instant = Instant.parse(databaseValue)

    override fun encode(value: Instant): String = value.toString()
}

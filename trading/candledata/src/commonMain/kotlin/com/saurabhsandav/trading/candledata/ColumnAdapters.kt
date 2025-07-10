package com.saurabhsandav.trading.candledata

import app.cash.sqldelight.ColumnAdapter
import kotlin.time.Instant

internal object InstantLongColumnAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant = Instant.Companion.fromEpochSeconds(databaseValue)

    override fun encode(value: Instant): Long = value.epochSeconds
}

package com.saurabhsandav.core.utils

import app.cash.sqldelight.ColumnAdapter
import kotlin.time.Instant

object InstantColumnAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant = Instant.fromEpochSeconds(databaseValue)

    override fun encode(value: Instant): Long = value.epochSeconds
}

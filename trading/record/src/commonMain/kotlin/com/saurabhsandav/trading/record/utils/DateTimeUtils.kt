package com.saurabhsandav.trading.record.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

internal fun Clock.nowIn(timeZone: TimeZone): LocalDateTime {
    return now().toLocalDateTime(timeZone)
}

internal fun Instant.withoutNanoseconds(): Instant = this - nanosecondsOfSecond.nanoseconds

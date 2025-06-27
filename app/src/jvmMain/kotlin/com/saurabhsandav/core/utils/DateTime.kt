package com.saurabhsandav.core.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

fun Clock.nowIn(timeZone: TimeZone): LocalDateTime {
    return now().toLocalDateTime(timeZone)
}

fun Instant.withoutNanoseconds(): Instant = this - nanosecondsOfSecond.nanoseconds

package com.saurabhsandav.core.utils

import kotlinx.datetime.*
import kotlin.time.Duration.Companion.nanoseconds

fun Clock.nowIn(timeZone: TimeZone): LocalDateTime {
    return now().toLocalDateTime(timeZone)
}

fun Instant.withoutNanoseconds(): Instant = this - nanosecondsOfSecond.nanoseconds

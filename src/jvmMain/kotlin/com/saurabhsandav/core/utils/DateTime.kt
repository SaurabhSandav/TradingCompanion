package com.saurabhsandav.core.utils

import kotlinx.datetime.*
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.nanoseconds

fun Clock.nowIn(timeZone: TimeZone): LocalDateTime {
    return now().toLocalDateTime(timeZone)
}

fun Instant.withoutNanoseconds(): Instant = this - nanosecondsOfSecond.nanoseconds

fun DateTimeFormatter.format(ldt: LocalDateTime): String {
    return format(ldt.toJavaLocalDateTime())
}

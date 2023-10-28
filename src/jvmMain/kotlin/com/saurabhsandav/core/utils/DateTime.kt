package com.saurabhsandav.core.utils

import kotlinx.datetime.*
import java.time.format.DateTimeFormatter

fun Clock.nowIn(timeZone: TimeZone): LocalDateTime {
    return now().toLocalDateTime(timeZone)
}

fun LocalDateTime.withoutNanoseconds(): LocalDateTime {
    return date.atTime(LocalTime(hour = hour, minute = minute, second = second))
}

fun DateTimeFormatter.format(dateTime: LocalDateTime): String {
    return format(dateTime.toJavaLocalDateTime())
}

package com.saurabhsandav.core.ui.common

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter

fun DateTimeFormatter.format(dateTime: LocalDateTime): String {
    return format(dateTime.toJavaLocalDateTime())
}

val TradeDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy - HH:mm:ss")

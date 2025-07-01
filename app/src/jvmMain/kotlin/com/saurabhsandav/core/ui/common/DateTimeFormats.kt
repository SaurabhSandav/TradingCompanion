package com.saurabhsandav.core.ui.common

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

val TradeDateTimeFormat: DateTimeFormat<LocalDateTime> = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(padding = Padding.NONE)
    chars(", ")
    year()
    chars(" - ")
    hour()
    char(':')
    minute()
    char(':')
    second()
}

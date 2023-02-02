package com.saurabhsandav.core.ui.common

import com.saurabhsandav.core.trading.Timeframe

val TimeframeLabels = listOf(
    "1M",
    "3M",
    "5M",
    "15M",
    "30M",
    "1H",
    "4H",
    "1D",
)

fun Timeframe.toLabel(): String = when (this) {
    Timeframe.M1 -> "1M"
    Timeframe.M3 -> "3M"
    Timeframe.M5 -> "5M"
    Timeframe.M15 -> "15M"
    Timeframe.M30 -> "30M"
    Timeframe.H1 -> "1H"
    Timeframe.H4 -> "4H"
    Timeframe.D1 -> "1D"
}

fun timeframeFromLabel(label: String): Timeframe = when (label) {
    "1M" -> Timeframe.M1
    "3M" -> Timeframe.M3
    "5M" -> Timeframe.M5
    "15M" -> Timeframe.M15
    "30M" -> Timeframe.M30
    "1H" -> Timeframe.H1
    "4H" -> Timeframe.H4
    "1D" -> Timeframe.D1
    else -> error("Invalid timeframe $label")
}

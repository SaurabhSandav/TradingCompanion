package com.saurabhsandav.core.ui.common

import com.saurabhsandav.core.trading.Timeframe

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

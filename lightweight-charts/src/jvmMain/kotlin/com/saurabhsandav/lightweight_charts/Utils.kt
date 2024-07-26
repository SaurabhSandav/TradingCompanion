package com.saurabhsandav.lightweight_charts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun Color.toHexString(): String {
    return toArgb().run { String.format("#%06X", 0xFFFFFF and this) }
}

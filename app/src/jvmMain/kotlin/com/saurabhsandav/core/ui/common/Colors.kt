package com.saurabhsandav.core.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object AppColor {

    val ProfitGreen = Color(red = 46, green = 204, blue = 113)

    val LossRed = Color(red = 231, green = 76, blue = 60)
}

fun Color.toHexString(): String {
    return toArgb().run { String.format("#%06X", 0xFFFFFF and this) }
}

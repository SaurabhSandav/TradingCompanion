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

fun Color.Companion.hex(hex: String): Color? {

    if (!hex.startsWith("#")) return null
    if (hex.length != 7) return null

    val colorInt = hex.substring(1)

    return try {

        val r = Integer.parseInt(colorInt.take(2), 16)
        val g = Integer.parseInt(colorInt.substring(2, 4), 16)
        val b = Integer.parseInt(colorInt.substring(4, 6), 16)

        Color(r, g, b)
    } catch (_: NullPointerException) {
        null
    }
}

fun Color.toCssColor() = kotlinx.css.Color(toHexString())

package com.saurabhsandav.core.ui.common

typealias ComposeColor = androidx.compose.ui.graphics.Color
typealias AwtColor = java.awt.Color

fun ComposeColor.toAwtColor(): AwtColor {
    @Suppress("ktlint:standard:comment-wrapping")
    return AwtColor(
        /* r = */ red,
        /* g = */ green,
        /* b = */ blue,
        /* a = */ alpha,
    )
}

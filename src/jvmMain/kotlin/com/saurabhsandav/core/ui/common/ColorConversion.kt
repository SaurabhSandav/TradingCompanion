package com.saurabhsandav.core.ui.common

typealias ComposeColor = androidx.compose.ui.graphics.Color
typealias JFXColor = javafx.scene.paint.Color
typealias AwtColor = java.awt.Color

fun ComposeColor.toAwtColor(): AwtColor {
    return AwtColor(
        /* r = */ red,
        /* g = */ green,
        /* b = */ blue,
        /* a = */ alpha,
    )
}

fun AwtColor.toJavaFxColor(): JFXColor {
    return JFXColor.rgb(
        /* red = */ red,
        /* green = */ green,
        /* blue = */ blue,
        /* opacity = */ alpha / 255.0
    )
}

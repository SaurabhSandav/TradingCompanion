package com.saurabhsandav.core.ui.common.app

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.WindowPlacement

class AppWindowConfig {

    var windowPlacement: WindowPlacement? = null
}

internal val LocalAppWindowConfig = staticCompositionLocalOf { AppWindowConfig() }

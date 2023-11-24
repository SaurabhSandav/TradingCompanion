package com.saurabhsandav.core.ui.settings.model

import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen

internal data class SettingsState(
    val darkModeEnabled: Boolean,
    val landingScreen: LandingScreen,
    val densityFraction: Float,
    val defaultTimeframe: Timeframe,
    val webViewBackend: WebViewBackend,
    val backupProgress: String?,
    val eventSink: (SettingsEvent) -> Unit,
)

enum class WebViewBackend {
    JavaFX,
    JCEF;
}

package com.saurabhsandav.core.ui.settings.model

import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen
import com.saurabhsandav.trading.core.Timeframe

internal data class SettingsState(
    val darkModeEnabled: Boolean,
    val landingScreen: LandingScreen,
    val densityFraction: Float,
    val defaultTimeframe: Timeframe,
    val eventSink: (SettingsEvent) -> Unit,
) {

    enum class Category {
        Layout,
        Trading,
        Backup,
    }
}

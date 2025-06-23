package com.saurabhsandav.core.ui.settings.model

import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen
import com.saurabhsandav.trading.core.Timeframe

internal sealed class SettingsEvent {

    data class ChangeDarkModeEnabled(
        val isEnabled: Boolean,
    ) : SettingsEvent()

    data class ChangeLandingScreen(
        val landingScreen: LandingScreen,
    ) : SettingsEvent()

    data class ChangeDensityFraction(
        val densityFraction: Float,
    ) : SettingsEvent()

    data class ChangeDefaultTimeframe(
        val timeframe: Timeframe,
    ) : SettingsEvent()
}

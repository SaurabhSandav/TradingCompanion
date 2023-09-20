package com.saurabhsandav.core.ui.settings.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen

@Immutable
internal data class SettingsState(
    val darkModeEnabled: Boolean,
    val landingScreen: LandingScreen,
    val densityFraction: Float,
    val eventSink: (SettingsEvent) -> Unit,
)

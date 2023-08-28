package com.saurabhsandav.core.ui.settings.model

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsState(
    val darkModeEnabled: Boolean,
    val landingScreen: String,
    val densityFraction: Float,
    val eventSink: (SettingsEvent) -> Unit,
)

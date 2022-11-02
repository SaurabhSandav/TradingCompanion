package ui.settings.model

sealed class SettingsEvent {

    data class ChangeLandingScreen(val landingScreen: String) : SettingsEvent()

    data class ChangeDensityFraction(val densityFraction: Float) : SettingsEvent()
}

package ui.landing.model

internal sealed class LandingEvent {

    data class ChangeCurrentScreen(val screen: LandingScreen) : LandingEvent()
}

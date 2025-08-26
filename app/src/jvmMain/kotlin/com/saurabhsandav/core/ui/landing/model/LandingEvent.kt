package com.saurabhsandav.core.ui.landing.model

internal sealed class LandingEvent {

    data class ChangeCurrentScreen(
        val screen: LandingScreen,
    ) : LandingEvent()
}

package com.saurabhsandav.core.ui.landing.model

import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen

internal sealed class LandingEvent {

    data class ChangeCurrentScreen(
        val screen: LandingScreen,
    ) : LandingEvent()
}

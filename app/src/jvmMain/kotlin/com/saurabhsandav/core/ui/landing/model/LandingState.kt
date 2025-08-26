package com.saurabhsandav.core.ui.landing.model

internal data class LandingState(
    val currentScreen: LandingScreen?,
    val openTradesCount: Int?,
    val eventSink: (LandingEvent) -> Unit,
)

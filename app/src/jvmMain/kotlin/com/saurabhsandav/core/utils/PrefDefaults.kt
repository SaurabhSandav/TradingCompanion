package com.saurabhsandav.core.utils

import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen.Account
import com.saurabhsandav.trading.core.Timeframe

object PrefDefaults {

    const val DarkModeEnabled = false
    internal val LandingScreen = Account
    const val DensityFraction = 0.8F
    val DefaultTimeframe = Timeframe.M5
}

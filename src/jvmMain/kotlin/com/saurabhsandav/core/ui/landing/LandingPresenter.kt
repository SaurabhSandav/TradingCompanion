package com.saurabhsandav.core.ui.landing

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.landing.model.LandingEvent
import com.saurabhsandav.core.ui.landing.model.LandingScreen
import com.saurabhsandav.core.ui.landing.model.LandingState
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Stable
internal class LandingPresenter(
    private val coroutineScope: CoroutineScope,
    appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
) {

    private var currentScreen by mutableStateOf<LandingScreen?>(null)

    init {

        coroutineScope.launch {
            currentScreen = appPrefs.getStringFlow(PrefKeys.LandingScreen, PrefDefaults.LandingScreen.name).first()
                .let(LandingScreen::valueOf)
        }
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule LandingState(
            currentScreen = currentScreen,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: LandingEvent) {

        when (event) {
            is LandingEvent.ChangeCurrentScreen -> onCurrentScreenChange(event.screen)
        }
    }

    private fun onCurrentScreenChange(screen: LandingScreen) {
        currentScreen = screen
    }
}

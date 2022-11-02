package ui.landing

import AppModule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ui.common.CollectEffect
import ui.landing.model.LandingEvent
import ui.landing.model.LandingScreen
import ui.landing.model.LandingState
import utils.PrefDefaults
import utils.PrefKeys

internal class LandingPresenter(
    private val coroutineScope: CoroutineScope,
    appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
) {

    private val events = MutableSharedFlow<LandingEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private var currentScreen by mutableStateOf<LandingScreen?>(null)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is LandingEvent.ChangeCurrentScreen -> onCurrentScreenChange(event.screen)
            }
        }

        return@launchMolecule LandingState(
            currentScreen = currentScreen,
        )
    }

    fun event(event: LandingEvent) {
        events.tryEmit(event)
    }

    init {

        coroutineScope.launch {
            currentScreen = appPrefs.getStringFlow(PrefKeys.LandingScreen, PrefDefaults.LandingScreen.name).first()
                .let(LandingScreen::valueOf)
        }
    }

    private fun onCurrentScreenChange(screen: LandingScreen) {
        currentScreen = screen
    }
}

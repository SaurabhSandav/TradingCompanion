package ui.settings

import AppModule
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import launchUnit
import ui.common.CollectEffect
import ui.landing.model.LandingScreen
import ui.settings.model.SettingsEvent
import ui.settings.model.SettingsState
import utils.PrefDefaults
import utils.PrefKeys

internal class SettingsPresenter(
    private val coroutineScope: CoroutineScope,
    appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
) {

    private val events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = Int.MAX_VALUE)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is SettingsEvent.ChangeLandingScreen -> onLandingScreenChange(event.landingScreen)
                is SettingsEvent.ChangeDensityFraction -> onDensityFractionChange(event.densityFraction)
            }
        }

        val landingScreen by remember {
            appPrefs.getStringFlow(PrefKeys.LandingScreen, PrefDefaults.LandingScreen.name)
                .map { LandingScreen.valueOf(it).title }
        }.collectAsState(PrefDefaults.LandingScreen.name)

        val densityFraction by appPrefs.getFloatFlow(PrefKeys.DensityFraction, PrefDefaults.DensityFraction)
            .collectAsState(PrefDefaults.DensityFraction)

        return@launchMolecule SettingsState(
            landingScreen = landingScreen,
            densityFraction = densityFraction,
        )
    }

    fun event(event: SettingsEvent) {
        events.tryEmit(event)
    }

    private fun onLandingScreenChange(landingScreen: String) = coroutineScope.launchUnit {
        appPrefs.putString(PrefKeys.LandingScreen, LandingScreen.items.first { it.title == landingScreen }.name)
    }

    private fun onDensityFractionChange(densityFraction: Float) = coroutineScope.launchUnit {
        appPrefs.putFloat(PrefKeys.DensityFraction, densityFraction)
    }
}

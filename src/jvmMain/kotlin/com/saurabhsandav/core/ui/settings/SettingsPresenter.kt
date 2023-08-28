package com.saurabhsandav.core.ui.settings

import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.landing.model.LandingScreen
import com.saurabhsandav.core.ui.settings.model.SettingsEvent
import com.saurabhsandav.core.ui.settings.model.SettingsEvent.*
import com.saurabhsandav.core.ui.settings.model.SettingsState
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

@Stable
internal class SettingsPresenter(
    private val coroutineScope: CoroutineScope,
    appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
) {

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        val darkModeEnabled by remember {
            appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled)
        }.collectAsState(PrefDefaults.DarkModeEnabled)

        val landingScreen by remember {
            appPrefs.getStringFlow(PrefKeys.LandingScreen, PrefDefaults.LandingScreen.name)
                .map { LandingScreen.valueOf(it).title }
        }.collectAsState(PrefDefaults.LandingScreen.name)

        val densityFraction by appPrefs.getFloatFlow(PrefKeys.DensityFraction, PrefDefaults.DensityFraction)
            .collectAsState(PrefDefaults.DensityFraction)

        return@launchMolecule SettingsState(
            darkModeEnabled = darkModeEnabled,
            landingScreen = landingScreen,
            densityFraction = densityFraction,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: SettingsEvent) {

        when (event) {
            is ChangeDarkModeEnabled -> onDarkModeEnabledChange(event.isEnabled)
            is ChangeLandingScreen -> onLandingScreenChange(event.landingScreen)
            is ChangeDensityFraction -> onDensityFractionChange(event.densityFraction)
        }
    }

    private fun onDarkModeEnabledChange(isEnabled: Boolean) = coroutineScope.launchUnit {
        appPrefs.putBoolean(PrefKeys.DarkModeEnabled, isEnabled)
    }

    private fun onLandingScreenChange(landingScreen: String) = coroutineScope.launchUnit {
        appPrefs.putString(PrefKeys.LandingScreen, LandingScreen.items.first { it.title == landingScreen }.name)
    }

    private fun onDensityFractionChange(densityFraction: Float) = coroutineScope.launchUnit {
        appPrefs.putFloat(PrefKeys.DensityFraction, densityFraction)
    }
}

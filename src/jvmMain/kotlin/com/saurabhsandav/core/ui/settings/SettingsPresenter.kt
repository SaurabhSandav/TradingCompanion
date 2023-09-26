package com.saurabhsandav.core.ui.settings

import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen
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
                .map { kotlin.runCatching { LandingScreen.valueOf(it) }.getOrNull() ?: PrefDefaults.LandingScreen }
        }.collectAsState(PrefDefaults.LandingScreen)

        val densityFraction by remember {
            appPrefs.getFloatFlow(PrefKeys.DensityFraction, PrefDefaults.DensityFraction)
        }.collectAsState(PrefDefaults.DensityFraction)

        val defaultTimeframe by remember {
            appPrefs.getStringFlow(PrefKeys.DefaultTimeframe, PrefDefaults.DefaultTimeframe.name)
                .map { kotlin.runCatching { Timeframe.valueOf(it) }.getOrNull() ?: PrefDefaults.DefaultTimeframe }
        }.collectAsState(PrefDefaults.DefaultTimeframe)

        return@launchMolecule SettingsState(
            darkModeEnabled = darkModeEnabled,
            landingScreen = landingScreen,
            densityFraction = densityFraction,
            defaultTimeframe = defaultTimeframe,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: SettingsEvent) {

        when (event) {
            is ChangeDarkModeEnabled -> onDarkModeEnabledChange(event.isEnabled)
            is ChangeLandingScreen -> onLandingScreenChange(event.landingScreen)
            is ChangeDensityFraction -> onDensityFractionChange(event.densityFraction)
            is ChangeDefaultTimeframe -> onDefaultTimeframeChange(event.timeframe)
        }
    }

    private fun onDarkModeEnabledChange(isEnabled: Boolean) = coroutineScope.launchUnit {
        appPrefs.putBoolean(PrefKeys.DarkModeEnabled, isEnabled)
    }

    private fun onLandingScreenChange(landingScreen: LandingScreen) = coroutineScope.launchUnit {
        appPrefs.putString(PrefKeys.LandingScreen, landingScreen.name)
    }

    private fun onDensityFractionChange(densityFraction: Float) = coroutineScope.launchUnit {
        appPrefs.putFloat(PrefKeys.DensityFraction, densityFraction)
    }

    private fun onDefaultTimeframeChange(defaultTimeframe: Timeframe) = coroutineScope.launchUnit {
        appPrefs.putString(PrefKeys.DefaultTimeframe, defaultTimeframe.name)
    }
}

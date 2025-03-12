package com.saurabhsandav.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.WindowPlacement
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AppConfig(
    private val scope: CoroutineScope,
    private val appPrefs: FlowSettings,
) {

    var densityFraction by mutableStateOf(PrefDefaults.DensityFraction)
        private set

    var isDarkModeEnabled by mutableStateOf(PrefDefaults.DarkModeEnabled)
        private set

    var windowPlacement: WindowPlacement? = null
        set(value) {

            field = value

            scope.launch {
                when (value) {
                    null -> appPrefs.remove(PrefKeys.WindowPlacement)
                    else -> appPrefs.putString(PrefKeys.WindowPlacement, value.name)
                }
            }
        }

    init {

        // Density
        appPrefs.getFloatFlow(PrefKeys.DensityFraction, PrefDefaults.DensityFraction)
            .onEach { densityFraction = it }
            .launchIn(scope)

        // Dark Mode
        appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled)
            .onEach { isDarkModeEnabled = it }
            .launchIn(scope)

        // WindowPlacement
        appPrefs.getStringOrNullFlow(PrefKeys.WindowPlacement)
            .onEach { windowPlacement = it?.let { WindowPlacement.valueOf(it) } }
            .launchIn(scope)
    }
}

@Composable
fun AppConfig.adjustedDensity(): Density {

    val density = LocalDensity.current

    return remember(density, densityFraction) {
        Density(density.density * densityFraction, density.fontScale)
    }
}

@Composable
fun AppConfig.originalDensity(): Density {

    val density = LocalDensity.current

    return remember(density, densityFraction) {
        Density(density.density / densityFraction, density.fontScale)
    }
}

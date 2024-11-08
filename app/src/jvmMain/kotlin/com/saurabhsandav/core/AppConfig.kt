package com.saurabhsandav.core

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AppConfig(
    scope: CoroutineScope,
    appPrefs: FlowSettings,
) {

    var densityFraction by mutableStateOf(PrefDefaults.DensityFraction)
        private set

    var isDarkModeEnabled by mutableStateOf(PrefDefaults.DarkModeEnabled)
        private set

    init {

        appPrefs.getFloatFlow(PrefKeys.DensityFraction, PrefDefaults.DensityFraction)
            .onEach { densityFraction = it }
            .launchIn(scope)

        appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled)
            .onEach { isDarkModeEnabled = it }
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

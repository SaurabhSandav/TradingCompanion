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
import com.saurabhsandav.core.di.AppCoroutineScope
import com.saurabhsandav.core.di.AppPrefs
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
@Inject
class AppConfig(
    @AppCoroutineScope private val scope: CoroutineScope,
    @AppPrefs private val appPrefs: FlowSettings,
    private val tradingProfiles: TradingProfiles,
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

    val currentTradingProfileFlow = appPrefs.getLongOrNullFlow(PrefKeys.CurrentTradingProfile)
        .flatMapLatest { profileId ->

            if (profileId == null) {

                // Current profile not set. Set default profile as current.
                setCurrentTradingProfileId(tradingProfiles.getDefaultProfile().first().id)

                // Return empty flow. New emission from prefs will follow.
                return@flatMapLatest emptyFlow()
            }

            val id = ProfileId(profileId)

            val profileExists = tradingProfiles.exists(id)

            if (!profileExists) {

                // Profile doesn't exist. Set default profile as current.
                setCurrentTradingProfileId(tradingProfiles.allProfiles.first().first().id)

                // Return empty flow. New emission from prefs will follow.
                return@flatMapLatest emptyFlow()
            }

            tradingProfiles.getProfile(id)
        }

    suspend fun getCurrentTradingProfile(): TradingProfile = currentTradingProfileFlow.first()

    suspend fun setCurrentTradingProfileId(id: ProfileId) {
        appPrefs.putLong(PrefKeys.CurrentTradingProfile, id.value)
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

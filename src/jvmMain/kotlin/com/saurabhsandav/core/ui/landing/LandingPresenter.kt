package com.saurabhsandav.core.ui.landing

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.landing.model.LandingEvent
import com.saurabhsandav.core.ui.landing.model.LandingState
import com.saurabhsandav.core.ui.landing.model.LandingState.LandingScreen
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class LandingPresenter(
    coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val appPrefs: FlowSettings,
    private val tradingProfiles: TradingProfiles,
) {

    private var currentScreen by mutableStateOf<LandingScreen?>(null)

    init {

        coroutineScope.launch {
            currentScreen = appPrefs.getStringFlow(PrefKeys.LandingScreen, PrefDefaults.LandingScreen.name)
                .first()
                .let { kotlin.runCatching { LandingScreen.valueOf(it) }.getOrNull() ?: PrefDefaults.LandingScreen }
        }
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule LandingState(
            currentScreen = currentScreen,
            openTradesCount = getOpenTradeCount(),
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: LandingEvent) {

        when (event) {
            is LandingEvent.ChangeCurrentScreen -> onCurrentScreenChange(event.screen)
        }
    }

    @Composable
    private fun getOpenTradeCount(): Int? {
        return produceState<Int?>(null) {

            flow {

                tradingProfiles
                    .getProfile(profileId)
                    .map { profile -> profile.tradeCountOpen.takeUnless { it == 0 } }
                    .emitInto(this)
            }.collect { value = it }
        }.value
    }

    private fun onCurrentScreenChange(screen: LandingScreen) {
        currentScreen = screen
    }
}

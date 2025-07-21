package com.saurabhsandav.core.ui.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppConfig
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.profiles.model.ProfilesEvent
import com.saurabhsandav.core.ui.profiles.model.ProfilesEvent.DeleteProfile
import com.saurabhsandav.core.ui.profiles.model.ProfilesEvent.SetCurrentProfile
import com.saurabhsandav.core.ui.profiles.model.ProfilesEvent.UpdateSelectedProfile
import com.saurabhsandav.core.ui.profiles.model.ProfilesState
import com.saurabhsandav.core.ui.profiles.model.ProfilesState.Profile
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class ProfilesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appConfig: AppConfig,
    private val tradingProfiles: TradingProfiles,
    private val customSelectionMode: Boolean,
    private val trainingOnly: Boolean,
    selectedProfileId: ProfileId? = null,
    private val onProfileSelected: ((ProfileId?) -> Unit)? = null,
) {

    private val currentProfileId = MutableStateFlow(selectedProfileId)

    init {

        // Handle selected profile deletion
        currentProfileId
            .filterNotNull()
            .flatMapLatest(tradingProfiles::getProfileOrNull)
            .filter { it == null }
            .onEach {
                currentProfileId.value = null
                onProfileSelected!!.invoke(null)
            }
            .launchIn(coroutineScope)
    }

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ProfilesState(
            profiles = getProfiles().value,
            currentProfile = getCurrentProfile().value,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: ProfilesEvent) {

        when (event) {
            is SetCurrentProfile -> onSetCurrentProfile(event.id)
            is UpdateSelectedProfile -> onUpdateSelectedProfile(event.id)
            is DeleteProfile -> onDeleteProfile(event.id)
        }
    }

    @Composable
    private fun getProfiles(): State<List<Profile>> {
        return remember {
            tradingProfiles.allProfiles.map { profiles ->
                profiles.filter { if (trainingOnly) it.isTraining else true }.map(::toProfileState)
            }
        }.collectAsState(emptyList())
    }

    @Composable
    private fun getCurrentProfile(): State<Profile?> {
        return remember {
            when {
                customSelectionMode -> currentProfileId.flatMapLatest { currentProfileId ->

                    if (currentProfileId == null) return@flatMapLatest flowOf(null)

                    tradingProfiles.getProfileOrNull(currentProfileId)
                }

                else -> appConfig.currentTradingProfileFlow
            }.map { profile -> profile?.let(::toProfileState) }
        }.collectAsState(null)
    }

    private fun onSetCurrentProfile(id: ProfileId) = coroutineScope.launchUnit {
        when {
            customSelectionMode -> {
                currentProfileId.value = id
                onProfileSelected?.invoke(id)
            }

            else -> appConfig.setCurrentTradingProfileId(id)
        }
    }

    private fun onUpdateSelectedProfile(id: ProfileId?) {
        currentProfileId.value = id
    }

    private fun onDeleteProfile(id: ProfileId) = coroutineScope.launchUnit {
        tradingProfiles.deleteProfile(id)
    }

    private fun toProfileState(profile: TradingProfile) = Profile(
        id = profile.id,
        name = profile.name,
        description = profile.description.takeIf { it.isNotBlank() },
        isTraining = profile.isTraining,
        tradeCount = profile.tradeCount,
        tradeCountOpen = profile.tradeCountOpen.takeIf { it > 0 },
    )

    interface Factory {

        fun build(
            customSelectionMode: Boolean,
            trainingOnly: Boolean,
            selectedProfileId: ProfileId? = null,
            onProfileSelected: ((ProfileId?) -> Unit)? = null,
        ): ProfilesPresenter
    }
}

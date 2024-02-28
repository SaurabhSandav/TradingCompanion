package com.saurabhsandav.core.ui.profiles

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.profiles.model.ProfilesEvent
import com.saurabhsandav.core.ui.profiles.model.ProfilesEvent.*
import com.saurabhsandav.core.ui.profiles.model.ProfilesState
import com.saurabhsandav.core.ui.profiles.model.ProfilesState.Profile
import com.saurabhsandav.core.utils.getCurrentTradingProfile
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.putCurrentTradingProfileId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

@Stable
internal class ProfilesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appPrefs: FlowSettings,
    private val tradingProfiles: TradingProfiles,
    private val customSelectionMode: Boolean,
    private val trainingOnly: Boolean,
) {

    private val currentProfileId = MutableStateFlow<ProfileId?>(null)

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
            is DeleteProfile -> onDeleteProfile(event.id)
            is CopyProfile -> onCopyProfile(event.id)
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
                    when (currentProfileId) {
                        null -> flowOf(null)
                        else -> tradingProfiles.getProfileOrNull(currentProfileId)
                    }
                }

                else -> appPrefs.getCurrentTradingProfile(tradingProfiles)
            }.filterNotNull().map(::toProfileState)
        }.collectAsState(null)
    }

    private fun onSetCurrentProfile(id: ProfileId) = coroutineScope.launchUnit {
        when {
            customSelectionMode -> currentProfileId.value = id
            else -> appPrefs.putCurrentTradingProfileId(id)
        }
    }

    private fun onDeleteProfile(id: ProfileId) = coroutineScope.launchUnit {
        tradingProfiles.deleteProfile(id)
    }

    private fun onCopyProfile(id: ProfileId) = coroutineScope.launchUnit {

        tradingProfiles.copyProfile(
            id = id,
            name = { "Copy of $it" },
        )
    }

    private fun toProfileState(profile: TradingProfile) = Profile(
        id = profile.id,
        name = profile.name,
        description = profile.description,
        isTraining = profile.isTraining,
    )

    fun interface Factory {

        operator fun invoke(
            customSelectionMode: Boolean,
            trainingOnly: Boolean,
        ): ProfilesPresenter
    }
}

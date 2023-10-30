package com.saurabhsandav.core.ui.profiles

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

@Stable
internal class ProfilesPresenter(
    private val coroutineScope: CoroutineScope,
    appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
    private val customSelectionMode: Boolean = false,
    private val trainingOnly: Boolean = false,
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
    private fun getProfiles(): State<ImmutableList<Profile>> {
        return remember {
            tradingProfiles.allProfiles.map { profiles ->
                profiles.filter { if (trainingOnly) it.isTraining else true }
                    .map(::toProfileState)
                    .toImmutableList()
            }
        }.collectAsState(persistentListOf())
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
}

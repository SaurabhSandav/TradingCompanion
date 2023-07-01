package com.saurabhsandav.core.ui.profiles

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.profiles.model.ProfileModel
import com.saurabhsandav.core.ui.profiles.model.ProfilesEvent
import com.saurabhsandav.core.ui.profiles.model.ProfilesEvent.*
import com.saurabhsandav.core.ui.profiles.model.ProfilesState
import com.saurabhsandav.core.ui.profiles.model.ProfilesState.Profile
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

@Stable
internal class ProfilesPresenter(
    private val coroutineScope: CoroutineScope,
    appModule: AppModule,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
    private val customSelectionMode: Boolean = false,
    private val trainingOnly: Boolean = false,
) {

    private val events = MutableSharedFlow<ProfilesEvent>(extraBufferCapacity = Int.MAX_VALUE)
    private val currentProfileId = MutableStateFlow<Long?>(null)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is CreateNewProfile -> onCreateNewProfile(event.profileModel)
                is SetCurrentProfile -> onSetCurrentProfile(event.id)
                is DeleteProfile -> onDeleteProfile(event.id)
                is UpdateProfile -> onUpdateProfile(event.id, event.profileModel)
                is CopyProfile -> onCopyProfile(event.id)
            }
        }

        return@launchMolecule ProfilesState(
            profiles = getProfiles().value,
            currentProfile = getCurrentProfile().value,
        )
    }

    fun event(event: ProfilesEvent) {
        events.tryEmit(event)
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

                else -> tradingProfiles.currentProfile
            }.filterNotNull().map(::toProfileState)
        }.collectAsState(null)
    }

    private fun onCreateNewProfile(profileModel: ProfileModel) = coroutineScope.launchUnit {

        tradingProfiles.newProfile(
            name = profileModel.name.value,
            description = profileModel.description.value,
            isTraining = profileModel.isTraining.value,
        )
    }

    private fun onSetCurrentProfile(id: Long) = coroutineScope.launchUnit {
        when {
            customSelectionMode -> currentProfileId.value = id
            else -> tradingProfiles.setCurrentProfile(id)
        }
    }

    private fun onDeleteProfile(id: Long) = coroutineScope.launchUnit {
        tradingProfiles.deleteProfile(id)
    }

    private fun onUpdateProfile(
        id: Long,
        profileModel: ProfileModel,
    ) = coroutineScope.launchUnit {

        tradingProfiles.updateProfile(
            id = id,
            name = profileModel.name.value,
            description = profileModel.description.value,
            isTraining = profileModel.isTraining.value,
        )
    }

    private fun onCopyProfile(id: Long) = coroutineScope.launchUnit {

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

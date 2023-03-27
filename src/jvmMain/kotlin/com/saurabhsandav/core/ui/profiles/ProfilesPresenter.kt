package com.saurabhsandav.core.ui.profiles

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine

@Stable
internal class ProfilesPresenter(
    private val coroutineScope: CoroutineScope,
    appModule: AppModule,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
) {

    private val events = MutableSharedFlow<ProfilesEvent>(extraBufferCapacity = Int.MAX_VALUE)

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

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
        )
    }

    fun event(event: ProfilesEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getProfiles(): State<ImmutableList<Profile>> {
        return remember {
            tradingProfiles.allProfiles.combine(tradingProfiles.currentProfile) { list, current ->
                list.map {
                    Profile(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        isTraining = it.isTraining,
                        isCurrent = it.id == current.id,
                    )
                }.toImmutableList()
            }
        }.collectAsState(persistentListOf())
    }

    private fun onCreateNewProfile(profileModel: ProfileModel) = coroutineScope.launchUnit {

        tradingProfiles.newProfile(
            name = profileModel.name.value,
            description = profileModel.description.value,
            isTraining = profileModel.isTraining.value,
        )
    }

    private fun onSetCurrentProfile(id: Long) = coroutineScope.launchUnit {
        tradingProfiles.setCurrentProfile(id)
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
}

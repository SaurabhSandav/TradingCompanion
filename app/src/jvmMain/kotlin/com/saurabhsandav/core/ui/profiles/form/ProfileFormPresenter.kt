package com.saurabhsandav.core.ui.profiles.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.record.TradingProfiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class ProfileFormPresenter(
    private val coroutineScope: CoroutineScope,
    private val onCloseRequest: () -> Unit,
    private val formType: ProfileFormType,
    private val trainingOnly: Boolean,
    private val tradingProfiles: TradingProfiles,
) {

    private var formModel by mutableStateOf<ProfileFormModel?>(null)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ProfileFormState(
            title = remember {
                when (formType) {
                    is ProfileFormType.New, is ProfileFormType.Copy -> "New Profile"
                    is ProfileFormType.Edit -> "Edit Profile"
                }
            },
            formModel = formModel,
            onSubmit = ::onSubmit,
        )
    }

    init {

        coroutineScope.launch {

            formModel = when (formType) {
                is ProfileFormType.New -> ProfileFormModel(
                    isProfileNameUnique = ::isProfileNameUnique,
                    isTraining = trainingOnly,
                )

                is ProfileFormType.Copy -> {

                    val profile = tradingProfiles.getProfile(formType.id).first()

                    ProfileFormModel(
                        isProfileNameUnique = ::isProfileNameUnique,
                        name = profile.name,
                        description = profile.description,
                        isTraining = profile.isTraining,
                    )
                }

                is ProfileFormType.Edit -> {

                    val profile = tradingProfiles.getProfileOrNull(formType.id).first()

                    // If profile already deleted, close
                    if (profile == null) {
                        onCloseRequest()
                        return@launch
                    }

                    ProfileFormModel(
                        isProfileNameUnique = ::isProfileNameUnique,
                        name = profile.name,
                        description = profile.description,
                        isTraining = profile.isTraining,
                    )
                }
            }
        }

        if (formType is ProfileFormType.Edit) {

            tradingProfiles
                .getProfileOrNull(formType.id)
                .onEach { profile -> if (profile == null) onCloseRequest() }
                .launchIn(coroutineScope)
        }
    }

    private fun onSubmit() = coroutineScope.launch {

        val formModel = formModel!!

        when (formType) {
            is ProfileFormType.New -> tradingProfiles.newProfile(
                name = formModel.nameField.value,
                description = formModel.descriptionField.value,
                isTraining = formModel.isTrainingField.value,
            )

            is ProfileFormType.Copy -> tradingProfiles.copyProfile(
                copyId = formType.id,
                name = formModel.nameField.value,
                description = formModel.descriptionField.value,
                isTraining = formModel.isTrainingField.value,
            )

            is ProfileFormType.Edit -> tradingProfiles.updateProfile(
                id = formType.id,
                name = formModel.nameField.value,
                description = formModel.descriptionField.value,
                isTraining = formModel.isTrainingField.value,
            )
        }

        onCloseRequest()
    }

    private suspend fun isProfileNameUnique(name: String): Boolean {
        return tradingProfiles.isProfileNameUnique(name, (formType as? ProfileFormType.Edit)?.id)
    }
}

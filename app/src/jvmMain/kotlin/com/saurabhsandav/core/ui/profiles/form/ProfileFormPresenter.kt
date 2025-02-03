package com.saurabhsandav.core.ui.profiles.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradingProfiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class ProfileFormPresenter(
    coroutineScope: CoroutineScope,
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
        )
    }

    init {

        coroutineScope.launch {

            formModel = ProfileFormModel(
                coroutineScope = coroutineScope,
                isProfileNameUnique = ::isProfileNameUnique,
                initial = when (formType) {
                    is ProfileFormType.New -> ProfileFormModel.Initial(isTraining = trainingOnly)
                    is ProfileFormType.Copy -> {

                        val profile = tradingProfiles.getProfile(formType.id).first()

                        ProfileFormModel.Initial(
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

                        ProfileFormModel.Initial(
                            name = profile.name,
                            description = profile.description,
                            isTraining = profile.isTraining,
                        )
                    }
                },
                onSubmit = { save() },
            )
        }

        if (formType is ProfileFormType.Edit) {

            tradingProfiles
                .getProfileOrNull(formType.id)
                .onEach { profile -> if (profile == null) onCloseRequest() }
                .launchIn(coroutineScope)
        }
    }

    private suspend fun ProfileFormModel.save() {

        when (formType) {
            is ProfileFormType.New -> tradingProfiles.newProfile(
                name = nameField.value,
                description = descriptionField.value,
                isTraining = isTrainingField.value,
            )

            is ProfileFormType.Copy -> tradingProfiles.copyProfile(
                copyId = formType.id,
                name = nameField.value,
                description = descriptionField.value,
                isTraining = isTrainingField.value,
            )

            is ProfileFormType.Edit -> tradingProfiles.updateProfile(
                id = formType.id,
                name = nameField.value,
                description = descriptionField.value,
                isTraining = isTrainingField.value,
            )
        }

        onCloseRequest()
    }

    private suspend fun isProfileNameUnique(name: String): Boolean {
        return tradingProfiles.isProfileNameUnique(name, (formType as? ProfileFormType.Edit)?.id)
    }
}

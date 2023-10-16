package com.saurabhsandav.core.ui.profiles.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class ProfileFormPresenter(
    private val coroutineScope: CoroutineScope,
    private val formType: ProfileFormType,
    private val onCloseRequest: () -> Unit,
    appModule: AppModule,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
) {

    private val formValidator = FormValidator(coroutineScope)

    private var formModel by mutableStateOf<ProfileFormModel?>(null)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule ProfileFormState(
            title = remember {
                when (formType) {
                    is ProfileFormType.New -> "New Profile"
                    is ProfileFormType.Edit -> "Edit Profile"
                }
            },
            formModel = formModel,
        )
    }

    init {

        coroutineScope.launch {

            formModel = ProfileFormModel(
                validator = formValidator,
                isProfileNameUnique = ::isProfileNameUnique,
                initial = when (formType) {
                    is ProfileFormType.New -> ProfileFormModel.Initial()
                    is ProfileFormType.Edit -> {

                        val profile = tradingProfiles.getProfile(formType.id).first()

                        ProfileFormModel.Initial(
                            name = profile.name,
                            description = profile.description,
                            isTraining = profile.isTraining,
                        )
                    }
                },
            )
        }
    }

    fun save() = coroutineScope.launchUnit {

        if (!formValidator.validate()) return@launchUnit

        val formModel = checkNotNull(formModel)

        when (formType) {
            is ProfileFormType.New -> tradingProfiles.newProfile(
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

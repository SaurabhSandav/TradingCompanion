package com.saurabhsandav.core.ui.profiles.form

import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.coroutines.CoroutineScope

internal data class ProfileFormState(
    val title: String,
    val formModel: ProfileFormModel?,
)

sealed class ProfileFormType {

    data object New : ProfileFormType()

    data class Edit(val id: ProfileId) : ProfileFormType()
}

internal class ProfileFormModel(
    coroutineScope: CoroutineScope,
    isProfileNameUnique: suspend (String) -> Boolean,
    initial: Initial,
    onSubmit: suspend ProfileFormModel.() -> Unit,
) {

    val validator = FormValidator(coroutineScope) { onSubmit() }

    val nameField = validator.addField(initial.name) {
        isRequired()
        if (length > 200) reportInvalid("Max 200 characters")
        if (!isProfileNameUnique(this)) reportInvalid("Name already taken")
    }

    val descriptionField = validator.addField(initial.description)

    val isTrainingField = validator.addField(initial.isTraining)

    class Initial(
        val name: String = "",
        val description: String = "",
        val isTraining: Boolean = false,
    )
}

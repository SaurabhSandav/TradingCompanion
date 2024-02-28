package com.saurabhsandav.core.ui.profiles.form

import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isRequired

internal data class ProfileFormState(
    val title: String,
    val formModel: ProfileFormModel?,
)

sealed class ProfileFormType {

    data object New : ProfileFormType()

    data class Edit(val id: ProfileId) : ProfileFormType()
}

internal class ProfileFormModel(
    val validator: FormValidator,
    isProfileNameUnique: suspend (String) -> Boolean,
    initial: Initial,
) {

    val nameField = validator.addField(initial.name) {
        isRequired()

        check(
            value = isProfileNameUnique(this),
            errorMessage = { "Profile already exists" }
        )
    }

    val descriptionField = validator.addField(initial.description)

    val isTrainingField = validator.addField(initial.isTraining)

    class Initial(
        val name: String = "",
        val description: String = "",
        val isTraining: Boolean = false,
    )
}

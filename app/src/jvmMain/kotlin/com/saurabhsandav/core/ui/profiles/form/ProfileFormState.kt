package com.saurabhsandav.core.ui.profiles.form

import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.adapter.addMutableStateField
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.core.ui.common.form.validations.isRequired

internal data class ProfileFormState(
    val title: String,
    val formModel: ProfileFormModel?,
    val onSubmit: () -> Unit,
)

sealed class ProfileFormType {

    data object New : ProfileFormType()

    data class Copy(
        val id: ProfileId,
    ) : ProfileFormType()

    data class Edit(
        val id: ProfileId,
    ) : ProfileFormType()
}

internal class ProfileFormModel(
    isProfileNameUnique: suspend (String) -> Boolean,
    name: String = "",
    description: String = "",
    isTraining: Boolean = false,
) : FormModel() {

    val nameField = addMutableStateField(name) {
        isRequired()
        if (length > 200) reportInvalid("Max 200 characters")
        if (!isProfileNameUnique(this)) reportInvalid("Name already taken")
    }

    val descriptionField = addMutableStateField(description)

    val isTrainingField = addMutableStateField(isTraining)
}

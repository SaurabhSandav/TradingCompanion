package com.saurabhsandav.core.ui.tagform.model

import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isRequired

internal data class TagFormState(
    val title: String,
    val formModel: TagFormModel?,
)

internal class TagFormModel(
    val validator: FormValidator,
    isTagNameUnique: suspend (String) -> Boolean,
    initial: Initial,
) {

    val nameField = validator.addField(initial.name) {
        isRequired()

        check(
            value = isTagNameUnique(this),
            errorMessage = { "Tag already exists" }
        )
    }

    val descriptionField = validator.addField(initial.description)

    class Initial(
        val name: String = "",
        val description: String = "",
    )
}

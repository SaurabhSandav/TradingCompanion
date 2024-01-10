package com.saurabhsandav.core.ui.tagform.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isRequired

@Immutable
internal data class TagFormState(
    val title: String,
    val formModel: TagFormModel?,
)

@Stable
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

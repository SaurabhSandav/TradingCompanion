package com.saurabhsandav.core.ui.tagform.model

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.coroutines.CoroutineScope

internal data class TagFormState(
    val title: String,
    val formModel: TagFormModel?,
)

internal class TagFormModel(
    coroutineScope: CoroutineScope,
    isTagNameUnique: suspend (String) -> Boolean,
    initial: Initial,
    onSubmit: suspend TagFormModel.() -> Unit,
) {

    val validator = FormValidator(coroutineScope) { onSubmit() }

    val nameField = validator.addField(initial.name) {
        isRequired()

        validate(
            isValid = isTagNameUnique(this),
            errorMessage = { "Tag already exists" }
        )
    }

    val descriptionField = validator.addField(initial.description)

    val colorField = validator.addField(initial.color)

    class Initial(
        val name: String = "",
        val description: String = "",
        val color: Color? = null,
    )
}

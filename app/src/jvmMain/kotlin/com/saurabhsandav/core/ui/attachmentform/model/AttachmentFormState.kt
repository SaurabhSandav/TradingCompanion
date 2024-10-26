package com.saurabhsandav.core.ui.attachmentform.model

import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import kotlinx.coroutines.CoroutineScope

internal data class AttachmentFormState(
    val formModel: AttachmentFormModel?,
)

internal class AttachmentFormModel(
    coroutineScope: CoroutineScope,
    initial: Initial,
    onSubmit: suspend AttachmentFormModel.() -> Unit,
) {

    val validator = FormValidator(coroutineScope) { onSubmit() }

    val nameField = validator.addField(initial.name) { isRequired() }

    val descriptionField = validator.addField(initial.description)

    var path = initial.path

    class Initial(
        val name: String = "",
        val description: String = "",
        val path: String? = null,
    )
}

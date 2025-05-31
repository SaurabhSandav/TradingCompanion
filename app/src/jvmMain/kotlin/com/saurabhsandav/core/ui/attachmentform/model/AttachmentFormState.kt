package com.saurabhsandav.core.ui.attachmentform.model

import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.validations.isRequired

internal data class AttachmentFormState(
    val formModel: AttachmentFormModel?,
    val onSubmit: () -> Unit,
)

internal class AttachmentFormModel(
    name: String = "",
    description: String = "",
    var path: String? = null,
) : FormModel() {

    val nameField = addField(name) { isRequired() }

    val descriptionField = addField(description)
}

package com.saurabhsandav.core.ui.attachmentform.model

import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.adapter.addMutableStateField
import com.saurabhsandav.core.ui.common.form.validations.isRequired

internal data class AttachmentFormState(
    val formModel: AttachmentFormModel?,
    val onSubmit: () -> Unit,
)

internal class AttachmentFormModel(
    name: String = "",
    description: String = "",
    path: String? = null,
) : FormModel() {

    val nameField = addMutableStateField(name) { isRequired() }

    val descriptionField = addMutableStateField(description)

    val pathField = addMutableStateField(path) { isRequired() }
}

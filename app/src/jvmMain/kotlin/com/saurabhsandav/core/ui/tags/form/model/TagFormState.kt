package com.saurabhsandav.core.ui.tags.form.model

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.adapter.addMutableStateField
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.core.ui.common.form.validations.isRequired

internal data class TagFormState(
    val title: String,
    val formModel: TagFormModel?,
    val onSubmit: () -> Unit,
)

internal class TagFormModel(
    isTagNameUnique: suspend (String) -> Boolean,
    name: String = "",
    description: String = "",
    color: Color? = null,
) : FormModel() {

    val nameField = addMutableStateField(name) {
        isRequired()

        if (!isTagNameUnique(this)) reportInvalid("Tag already exists")
    }

    val descriptionField = addMutableStateField(description)

    val colorField = addMutableStateField(color)
}

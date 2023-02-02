package com.saurabhsandav.core.ui.common.form.fields

import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.Validation

private class ListSelectionField(
    initial: String?,
    validations: Set<Validation<String?>> = emptySet(),
    private val isRequired: Boolean = true,
) : BaseFormField<String?>(initial, validations) {

    override fun validate() {

        when {
            isRequired && value == null -> {
                isValid = false
                errorMessage = "Required"
            }

            !isRequired && value == null -> Unit

            else -> super.validate()
        }
    }
}

fun FormValidator.listSelectionField(
    initial: String?,
    isRequired: Boolean = true,
    validations: Set<Validation<String?>> = emptySet(),
): FormField<String?> {

    val field = ListSelectionField(
        initial = initial,
        isRequired = isRequired,
        validations = validations,
    )

    addField(field)

    return field
}

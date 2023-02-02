package com.saurabhsandav.core.ui.common.form.fields

import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.Validation

private class TextField(
    initial: String,
    validations: Set<Validation<String>> = emptySet(),
    private val isRequired: Boolean = true,
) : BaseFormField<String>(initial, validations) {

    override fun validate() {

        when {
            isRequired && value.isBlank() -> {
                isValid = false
                errorMessage = "Required"
            }

            !isRequired && value.isBlank() -> Unit

            else -> super.validate()
        }
    }
}

fun FormValidator.textField(
    initial: String,
    validations: Set<Validation<String>> = emptySet(),
    isRequired: Boolean = true,
): FormField<String> {

    val field = TextField(
        initial = initial,
        isRequired = isRequired,
        validations = validations,
    )

    addField(field)

    return field
}

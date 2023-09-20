package com.saurabhsandav.core.ui.common.form.fields

import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.Validation

private class ListSelectionField<T : Any>(
    initial: T?,
    validations: Set<Validation<T?>> = emptySet(),
    private val isRequired: Boolean = true,
) : BaseFormField<T?>(initial, validations) {

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

fun <T : Any> FormValidator.listSelectionField(
    initial: T?,
    isRequired: Boolean = true,
    validations: Set<Validation<T?>> = emptySet(),
): FormField<T?> {

    val field = ListSelectionField(
        initial = initial,
        isRequired = isRequired,
        validations = validations,
    )

    addField(field)

    return field
}

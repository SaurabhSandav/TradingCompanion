package ui.common.form.fields

import kotlinx.datetime.LocalDateTime
import ui.common.form.FormField
import ui.common.form.FormValidator
import ui.common.form.Validation

fun FormValidator.switch(
    initial: Boolean,
    validations: Set<Validation<Boolean>> = emptySet(),
): FormField<Boolean> {

    val field = BaseFormField(initial, validations)

    addField(field)

    return field
}

fun FormValidator.dateTimeField(
    initial: LocalDateTime,
    validations: Set<Validation<LocalDateTime>> = emptySet(),
): FormField<LocalDateTime> {

    val field = BaseFormField(initial, validations)

    addField(field)

    return field
}

package com.saurabhsandav.core.ui.common.form.fields

import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.Validation
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

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

fun FormValidator.dateField(
    initial: LocalDate,
    validations: Set<Validation<LocalDate>> = emptySet(),
): FormField<LocalDate> {

    val field = BaseFormField(initial, validations)

    addField(field)

    return field
}

fun FormValidator.timeField(
    initial: LocalTime,
    validations: Set<Validation<LocalTime>> = emptySet(),
): FormField<LocalTime> {

    val field = BaseFormField(initial, validations)

    addField(field)

    return field
}

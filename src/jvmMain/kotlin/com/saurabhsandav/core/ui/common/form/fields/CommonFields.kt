package com.saurabhsandav.core.ui.common.form.fields

import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.Validation
import kotlinx.datetime.LocalDateTime

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

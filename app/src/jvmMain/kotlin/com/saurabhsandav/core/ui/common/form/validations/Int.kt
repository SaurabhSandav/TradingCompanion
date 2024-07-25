package com.saurabhsandav.core.ui.common.form.validations

import com.saurabhsandav.core.ui.common.form.Validation
import com.saurabhsandav.core.ui.common.form.ValidationScope

context(ValidationScope)
suspend fun String.isInt(
    validation: Validation<Int>? = null,
) {

    ifBlank { return }

    val intValue = toIntOrNull()

    intValue.isRequired { "Not a valid integer" }

    validation?.apply {
        intValue.validate()
    }
}

context(ValidationScope)
fun Int.isPositive() {
    validate(this > 0) { "Cannot be 0 or negative" }
}

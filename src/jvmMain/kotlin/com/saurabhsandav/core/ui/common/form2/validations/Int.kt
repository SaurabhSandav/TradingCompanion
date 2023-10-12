package com.saurabhsandav.core.ui.common.form2.validations

import com.saurabhsandav.core.ui.common.form2.Validation
import com.saurabhsandav.core.ui.common.form2.ValidationScope

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
    check(this > 0) { "Cannot be 0 or negative" }
}

package com.saurabhsandav.core.ui.common.form.validations

import com.saurabhsandav.core.ui.common.form.ValidationScope
import com.saurabhsandav.core.ui.common.form.reportInvalid

context(_: ValidationScope)
fun String.isInt(errorMessage: () -> String = { "Not a valid integer" }): Int? {

    val intValue = toIntOrNull()

    if (intValue == null) reportInvalid(message = errorMessage())

    return intValue
}

context(_: ValidationScope)
fun Int.isPositive(errorMessage: () -> String = { "Cannot be 0 or negative" }): Int {
    if (this < 0) reportInvalid(message = errorMessage())
    return this
}

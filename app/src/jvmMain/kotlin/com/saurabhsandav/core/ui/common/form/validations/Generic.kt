package com.saurabhsandav.core.ui.common.form.validations

import com.saurabhsandav.core.ui.common.form.ValidationScope
import com.saurabhsandav.core.ui.common.form.finishValidation
import com.saurabhsandav.core.ui.common.form.reportInvalid

context(_: ValidationScope)
inline fun <reified T> T?.isRequired(
    value: Boolean = true,
    noinline errorMessage: () -> String = { "Required" },
) {

    if (this != null) return
    if (!value) finishValidation()

    reportInvalid(errorMessage())
}

context(_: ValidationScope)
inline fun String.isRequired(
    value: Boolean = true,
    errorMessage: () -> String = { "Required" },
) {

    if (isNotBlank()) return
    if (!value) finishValidation()

    reportInvalid(errorMessage())
}

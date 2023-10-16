package com.saurabhsandav.core.ui.common.form.validations

import com.saurabhsandav.core.ui.common.form.ValidationScope
import kotlin.contracts.contract

context(ValidationScope)
inline fun <reified T> T?.isRequired(
    noinline errorMessage: () -> String = { "Required" },
) {

    contract {
        returns() implies (this@isRequired != null)
    }

    check(
        value = when (this) {
            is String -> isNotBlank()
            else -> this != null
        },
        errorMessage = errorMessage,
    )
}

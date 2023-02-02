package com.saurabhsandav.core.ui.common.form

class Validation<T>(
    val errorMessage: () -> String,
    val dependsOn: Set<FormField<*>> = emptySet(),
    val isValid: (T) -> Boolean,
)

fun <T> Validation(
    errorMessage: String,
    dependsOn: Set<FormField<*>> = emptySet(),
    isValid: (T) -> Boolean,
): Validation<T> = Validation(
    errorMessage = { errorMessage },
    dependsOn = dependsOn,
    isValid = isValid,
)

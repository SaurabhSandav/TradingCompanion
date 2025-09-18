package com.saurabhsandav.core.ui.common.form

fun interface Validation<T> {

    context(_: ValidationScope)
    suspend fun T.validate()
}

internal suspend fun <T> runValidation(
    value: T,
    validation: Validation<T>,
): Pair<ValidationResult, Set<FormField<*, *>>> {

    val scope = ValidationScopeImpl()

    try {
        with(scope) {
            with(validation) {
                value.validate()
            }
        }
    } catch (_: ValidationInterruptedException) {
        // Validation interrupted. Validation result is in `ValidationScopeImpl`.
    }

    return scope.result to scope.dependencies
}

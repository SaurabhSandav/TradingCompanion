package com.saurabhsandav.core.ui.common.form

import kotlin.coroutines.cancellation.CancellationException

interface ValidationScope {

    fun check(value: Boolean, errorMessage: () -> String)

    suspend fun <T> validated(formField: FormField<T>): T
}

internal sealed class ValidationResult {

    data object Valid : ValidationResult()

    data object DependencyInvalid : ValidationResult()

    data class Invalid(val errorMessage: String) : ValidationResult()
}

internal class ValidationScopeImpl : ValidationScope {

    var result: ValidationResult = ValidationResult.Valid
    val dependencies = mutableSetOf<FormField<*>>()

    override fun check(
        value: Boolean,
        errorMessage: () -> String,
    ) {

        if (!value) {

            result = ValidationResult.Invalid(errorMessage())

            throw ValidationException
        }
    }

    override suspend fun <T> validated(formField: FormField<T>): T {

        dependencies += formField

        if (!formField.validate()) {

            result = ValidationResult.DependencyInvalid

            throw ValidationException
        }

        return formField.value
    }
}

internal object ValidationException : CancellationException(null as String?) {

    private fun readResolve(): Any = ValidationException
}

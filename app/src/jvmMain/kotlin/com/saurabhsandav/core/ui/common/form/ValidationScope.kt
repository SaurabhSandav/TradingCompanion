package com.saurabhsandav.core.ui.common.form

import kotlin.coroutines.cancellation.CancellationException

interface ValidationScope {

    fun reportInvalid(message: String)

    suspend fun <T> validatedValue(field: FormField<*, T>): T

    /**
     * Run validation without interrupting on failed validations.
     */
    suspend fun collect(block: suspend context(ValidationScope) () -> Unit)

    fun finishValidation(): Nothing
}

context(scope: ValidationScope)
fun reportInvalid(message: String) = scope.reportInvalid(message)

context(scope: ValidationScope)
suspend fun <T> FormField<*, T>.validatedValue(): T = scope.validatedValue(this)

/**
 * Run validation without interrupting on failed validations.
 */
context(scope: ValidationScope)
suspend fun collect(block: suspend context(ValidationScope) () -> Unit) = scope.collect(block)

context(scope: ValidationScope)
fun finishValidation(): Nothing = scope.finishValidation()

internal sealed class ValidationResult {

    data object Valid : ValidationResult()

    data object DependencyInvalid : ValidationResult()

    data class Invalid(
        val errorMessages: List<String>,
    ) : ValidationResult()
}

internal class ValidationScopeImpl : ValidationScope {

    var result: ValidationResult = ValidationResult.Valid
    val dependencies = mutableSetOf<FormField<*, *>>()

    private var stopValidationOnInvalid = true

    override fun reportInvalid(message: String) {

        when (val currentResult = result) {
            is ValidationResult.Invalid -> (currentResult.errorMessages as MutableList<String>).add(message)
            else -> result = ValidationResult.Invalid(mutableListOf(message))
        }

        if (stopValidationOnInvalid) throw ValidationInterruptedException
    }

    override suspend fun collect(block: suspend context(ValidationScope) () -> Unit) {
        stopValidationOnInvalid = false
        block(this)
        stopValidationOnInvalid = true
    }

    override fun finishValidation(): Nothing {
        throw ValidationInterruptedException
    }

    override suspend fun <T> validatedValue(field: FormField<*, T>): T {

        dependencies += field

        if (!field.validate()) {

            result = ValidationResult.DependencyInvalid

            throw ValidationInterruptedException
        }

        return (field as FormFieldImpl).value
    }
}

internal object ValidationInterruptedException : CancellationException(null as String?) {

    @Suppress("unused")
    private fun readResolve(): Any = ValidationInterruptedException

    override fun fillInStackTrace(): Throwable = this
}

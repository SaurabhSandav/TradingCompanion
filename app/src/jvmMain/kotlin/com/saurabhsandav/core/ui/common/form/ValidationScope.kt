package com.saurabhsandav.core.ui.common.form

import kotlin.coroutines.cancellation.CancellationException

interface ValidationScope {

    fun reportInvalid(message: String)

    suspend fun <T> validated(formField: FormField<T>): T

    /**
     * Run validation without interrupting on failed validations.
     */
    suspend fun collect(scope: suspend context(ValidationScope) () -> Unit)

    fun finishValidation()
}

internal sealed class ValidationResult {

    data object Valid : ValidationResult()

    data object DependencyInvalid : ValidationResult()

    data class Invalid(val errorMessages: List<String>) : ValidationResult()
}

internal class ValidationScopeImpl : ValidationScope {

    var result: ValidationResult = ValidationResult.Valid
    val dependencies = mutableSetOf<FormField<*>>()

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

    override fun finishValidation() {
        throw ValidationInterruptedException
    }

    override suspend fun <T> validated(formField: FormField<T>): T {

        dependencies += formField

        if (!formField.validate()) {

            result = ValidationResult.DependencyInvalid

            throw ValidationInterruptedException
        }

        return formField.value
    }
}

internal object ValidationInterruptedException : CancellationException(null as String?) {

    @Suppress("unused")
    private fun readResolve(): Any = ValidationInterruptedException

    override fun fillInStackTrace(): Throwable = this
}

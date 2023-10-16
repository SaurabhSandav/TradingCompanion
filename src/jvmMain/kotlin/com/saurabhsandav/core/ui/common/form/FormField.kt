package com.saurabhsandav.core.ui.common.form

import androidx.compose.runtime.*
import com.saurabhsandav.core.ui.common.form.ValidationResult.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

interface FormField<T> : MutableState<T> {

    val errorMessage: String?

    val isValid: Boolean

    suspend fun validate(): Boolean

    fun registerDependent(field: FormField<*>)

    fun unregisterDependent(field: FormField<*>)
}

inline val FormField<*>.isError: Boolean
    get() = !isValid

internal class FormFieldImpl<T> internal constructor(
    initial: T,
    val coroutineScope: CoroutineScope,
    private val validation: Validation<T>?,
) : FormField<T> {

    // When no Validation is provided, isUpToDate is always true.
    private var isUpToDate = validation == null
    private val dependents = mutableSetOf<FormField<*>>()
    private var dependencies = setOf<FormField<*>>()

    override var errorMessage: String? by mutableStateOf(null)
        private set

    override var isValid by mutableStateOf(true)
        private set

    private val _value = mutableStateOf(initial)
    override var value: T by _value

    init {

        // Validate on every value change
        snapshotFlow { value }
            .drop(1) // Don't validate initial value
            .onEach {
                forceValidate()
                dependents.forEach { (it as FormFieldImpl).forceValidate() }
            }
            .launchIn(coroutineScope)
    }

    override suspend fun validate(): Boolean {

        // If not validation provided, field is always valid
        validation ?: return true

        if (isUpToDate) return isValid

        val receiver = ValidationScopeImpl()

        try {
            with(receiver) {
                with(validation) {
                    value.validate()
                }
            }
        } catch (ex: Exception) {

            when (ex) {
                ValidationException, DependencyValidationException -> Unit
                else -> throw ex
            }
        }

        val result = receiver.result

        val (isValid, errorMessage) = when (result) {
            is Invalid -> false to result.errorMessage
            DependencyInvalid, Valid -> true to null
        }

        this.isValid = isValid
        this.errorMessage = errorMessage

        // Update dependencies
        dependencies.forEach { it.unregisterDependent(this) }
        dependencies = receiver.dependencies
        dependencies.forEach { it.registerDependent(this) }

        isUpToDate = true

        return result == Valid
    }

    private suspend fun forceValidate() {
        isUpToDate = false
        validate()
    }

    override fun component1(): T = _value.component1()

    override fun component2(): (T) -> Unit = _value.component2()

    override fun registerDependent(field: FormField<*>) {
        dependents.add(field)
    }

    override fun unregisterDependent(field: FormField<*>) {
        dependents.remove(field)
    }
}

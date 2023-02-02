package com.saurabhsandav.core.ui.common.form.fields

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.Validation

open class BaseFormField<T> internal constructor(
    initial: T,
    final override val validations: Set<Validation<T>>,
) : FormField<T> {

    // If this FormField depends on a value that doesn't need to be validated, the value might be wrapped in a
    // FormField with no validations. In such cases, isUpToDate will be initialized to true.
    private var isUpToDate = validations.isEmpty()
    private val _value = mutableStateOf(initial)
    private val dependents = mutableSetOf<FormField<*>>()

    final override var errorMessage: String? by mutableStateOf(null)
        protected set

    final override var isValid by mutableStateOf(true)
        protected set

    override var value: T
        get() = _value.value
        set(value) {
            _value.value = value
            forceValidate()
            dependents.forEach {
                when (it) {
                    is BaseFormField<*> -> it.forceValidate()
                    else -> it.validate()
                }
            }
        }

    override fun component1(): T = _value.component1()

    override fun component2(): (T) -> Unit = _value.component2()

    override fun validate() {

        if (isUpToDate) return

        isUpToDate = true

        val firstFailedValidation = validations.filter { validation ->
            // Run validation only if dependencies have been validated
            when {
                validation.dependsOn.isNotEmpty() -> validation.dependsOn.all {
                    it.validate()
                    it.isValid
                }

                else -> true
            }
        }.firstOrNull { !it.isValid(value) }

        when (firstFailedValidation) {
            null -> {
                isValid = true
                errorMessage = null
            }

            else -> {
                isValid = false
                errorMessage = firstFailedValidation.errorMessage()
            }
        }
    }

    override fun registerDependent(state: FormField<*>) {
        dependents.add(state)
    }

    override fun unregisterDependent(state: FormField<*>) {
        dependents.remove(state)
    }

    private fun forceValidate() {
        isUpToDate = false
        validate()
    }
}

package ui.common.form

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ValidatableState<T> internal constructor(
    initial: T,
    private val validations: Set<Validation<T>>,
    internal val dependsOn: Set<ValidatableState<*>>,
) : MutableState<T> {

    // If a field depends on a value that doesn't need to be validated, the value might be wrapped in a
    // ValidatableState with no validations. In such cases, isReady will be initialized to true.
    private var isReady = validations.isEmpty()
    private val _value = mutableStateOf(initial)
    private val dependents = mutableSetOf<ValidatableState<*>>()

    var errorMessage: String? by mutableStateOf(null)
        private set

    var isValid by mutableStateOf(true)
        private set

    override var value: T
        get() = _value.value
        set(value) {
            _value.value = value
            validate()
            dependents.forEach(ValidatableState<*>::validateIfReady)
        }

    override fun component1(): T = _value.component1()

    override fun component2(): (T) -> Unit = _value.component2()

    fun validate() {

        isReady = true

        val firstFailedValidation = validations.filter { validation ->
            when {
                // Run validation only if dependencies have been validated
                validation.validateDependencies -> dependsOn.all { it.isReady && it.isValid }
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
                errorMessage = firstFailedValidation.errorMessage
            }
        }
    }

    internal fun registerDependent(state: ValidatableState<*>) {
        dependents.add(state)
    }

    internal fun unregisterDependent(state: ValidatableState<*>) {
        dependents.remove(state)
    }

    private fun validateIfReady() {
        if (isReady) validate()
    }
}

inline val ValidatableState<*>.isError: Boolean
    get() = !isValid

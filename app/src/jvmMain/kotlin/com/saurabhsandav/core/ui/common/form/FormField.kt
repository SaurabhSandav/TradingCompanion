package com.saurabhsandav.core.ui.common.form

import androidx.compose.runtime.*
import com.saurabhsandav.core.ui.common.form.ValidationResult.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

interface FormField<T> : MutableState<T> {

    val errorMessages: List<String>

    val isValid: Boolean

    suspend fun validate(): Boolean
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
    private var dependencies = mutableStateListOf<FormField<*>>()

    override var errorMessages = mutableStateListOf<String>()

    override var isValid by mutableStateOf(true)
        private set

    private val _value = mutableStateOf(initial)
    override var value: T
        get() = _value.value
        set(value) {
            _value.value = value
            // isValid is stale after value change.
            isUpToDate = false
        }

    init {

        coroutineScope.launch {

            // Validate on value change
            snapshotFlow { value }
                .drop(1) // Don't validate initial value
                .debounce(400.milliseconds)
                .collectLatest { forceValidate() }
        }

        coroutineScope.launch {

            // Validate on dependency value change
            snapshotFlow { dependencies.toList() }
                .flatMapLatest { fields ->

                    combineTransform(
                        flows = fields.map { field -> snapshotFlow { field.value } },
                        transform = { emit(Unit) },
                    )
                }
                .debounce(400.milliseconds)
                .collectLatest { forceValidate() }
        }
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
        } catch (_: ValidationInterruptedException) {
            // Validation interrupted. Validation result is in `ValidationScopeImpl`.
        }

        val result = receiver.result

        val (isValid, errorMessages) = when (result) {
            is Invalid -> false to result.errorMessages
            DependencyInvalid, Valid -> true to emptyList()
        }

        this.isValid = isValid
        this.errorMessages.apply {
            clear()
            addAll(errorMessages)
        }

        // Update dependencies
        dependencies.clear()
        dependencies.addAll(receiver.dependencies)

        isUpToDate = true

        return result == Valid
    }

    private suspend fun forceValidate() {
        isUpToDate = false
        validate()
    }

    override fun component1(): T = _value.component1()

    override fun component2(): (T) -> Unit = _value.component2()
}

package com.saurabhsandav.core.ui.common.form

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.saurabhsandav.core.ui.common.form.ValidationResult.DependencyInvalid
import com.saurabhsandav.core.ui.common.form.ValidationResult.Invalid
import com.saurabhsandav.core.ui.common.form.ValidationResult.Valid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds

interface FormField<T> : MutableState<T> {

    val errorMessages: List<String>

    val isValid: Boolean

    suspend fun validate(): Boolean

    fun autoValidateIn(coroutineScope: CoroutineScope)
}

inline val FormField<*>.isError: Boolean
    get() = !isValid

fun <T> FormField(
    initial: T,
    validation: Validation<T>?,
): FormField<T> = FormFieldImpl(initial, validation)

internal class FormFieldImpl<T> internal constructor(
    initial: T,
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

    override suspend fun validate(): Boolean {

        // If not validation provided, field is always valid
        validation ?: return true

        if (isUpToDate) return isValid

        val (result, dependencies) = runValidation(value, validation)

        val (isValid, errorMessages) = when (result) {
            is Invalid -> false to result.errorMessages
            DependencyInvalid, Valid -> true to emptyList()
        }

        this.isValid = isValid
        this.errorMessages.apply {
            clear()
            addAll(errorMessages)
        }
        this.dependencies.apply {
            clear()
            addAll(dependencies)
        }

        isUpToDate = true

        return result == Valid
    }

    private suspend fun forceValidate() {
        isUpToDate = false
        validate()
    }

    override fun autoValidateIn(coroutineScope: CoroutineScope) {

        // Validate on value change
        snapshotFlow { value }
            .drop(1) // Don't validate initial value
            .debounce(DebounceDuration)
            .onEach { forceValidate() }
            .launchIn(coroutineScope)

        // Validate on dependency value change
        snapshotFlow { dependencies.toList() }
            .flatMapLatest { fields ->

                combineTransform(
                    flows = fields.map { field -> snapshotFlow { field.value } },
                    transform = { emit(Unit) },
                )
            }
            .debounce(DebounceDuration)
            .onEach { forceValidate() }
            .launchIn(coroutineScope)
    }

    override fun component1(): T = _value.component1()

    override fun component2(): (T) -> Unit = _value.component2()
}

private val DebounceDuration = 400.milliseconds

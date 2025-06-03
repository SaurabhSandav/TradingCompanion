package com.saurabhsandav.core.ui.common.form

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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

    private var version = 0

    // When no Validation is provided, isUpToDate is always true.
    private var isUpToDate = validation == null
    private var dependencies = mutableStateMapOf<FormField<*>, Int>()

    override var errorMessages = mutableStateListOf<String>()

    override var isValid by mutableStateOf(true)
        private set

    private val _value = mutableStateOf(initial)
    override var value: T
        get() = _value.value
        set(value) {
            if (_value.value != value) {
                version++
                isUpToDate = false
                _value.value = value
            }
        }

    override suspend fun validate(): Boolean {

        // If not validation provided, field is always valid
        validation ?: return true

        if (isUpToDate && areDependenciesUpToDate()) return isValid

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
            dependencies.forEach {
                put(it as FormFieldImpl, it.version)
            }
        }

        isUpToDate = true

        return result == Valid
    }

    private fun areDependenciesUpToDate(): Boolean {

        dependencies.forEach { (field, version) ->
            if ((field as FormFieldImpl).version != version) return false
        }

        return true
    }

    override fun autoValidateIn(coroutineScope: CoroutineScope) {

        // Validate on value change
        snapshotFlow { value }
            .drop(1) // Don't validate initial value
            .debounce(DebounceDuration)
            .onEach { validate() }
            .launchIn(coroutineScope)

        // Validate on dependency value change
        snapshotFlow { dependencies.toList() }
            .flatMapLatest { fields ->

                combineTransform(
                    flows = fields.map { field -> snapshotFlow { field.first.value } },
                    transform = { emit(Unit) },
                )
            }
            .debounce(DebounceDuration)
            .onEach { validate() }
            .launchIn(coroutineScope)
    }

    override fun component1(): T = _value.component1()

    override fun component2(): (T) -> Unit = _value.component2()
}

private val DebounceDuration = 400.milliseconds

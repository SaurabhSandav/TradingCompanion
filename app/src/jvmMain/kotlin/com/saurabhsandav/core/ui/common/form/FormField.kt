package com.saurabhsandav.core.ui.common.form

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds

interface FormField<H, V> {

    val holder: H

    val value: V

    val errorMessages: List<String>

    val isValid: Boolean

    suspend fun validate(): Pair<V, Boolean>

    fun autoValidateIn(coroutineScope: CoroutineScope)

    interface Adapter<H, V> {

        fun getValue(holder: H): V

        fun getFlow(holder: H): Flow<V>
    }
}

inline val FormField<*, *>.isError: Boolean
    get() = !isValid

fun <H, V> FormField(
    holder: H,
    adapter: FormField.Adapter<H, V>,
    validation: Validation<V>?,
): FormField<H, V> = FormFieldImpl(holder, adapter, validation)

internal class FormFieldImpl<H, V> internal constructor(
    override val holder: H,
    private val adapter: FormField.Adapter<H, V>,
    private val validation: Validation<V>?,
) : FormField<H, V> {

    private var initialValidationFinished = false
    private var version = 0
    private var prevValue: V = adapter.getValue(holder)
    private val valueFlow = adapter.getFlow(holder)

    private var dependencies = mutableStateMapOf<FormField<*, *>, Int>()

    override val value: V
        get() = adapter.getValue(holder)

    override var errorMessages = mutableStateListOf<String>()

    override var isValid by mutableStateOf(true)
        private set

    override suspend fun validate(): Pair<V, Boolean> {

        // If no validation provided, field is always valid
        validation ?: return value to true

        val isValueSame = initialValidationFinished && prevValue == value
        if (isValueSame && areDependenciesUpToDate()) return value to isValid

        initialValidationFinished = true
        prevValue = value
        version++

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

        return value to (result == Valid)
    }

    private suspend fun areDependenciesUpToDate(): Boolean {

        dependencies.forEach { (field, version) ->
            val field = (field as FormFieldImpl)
            field.validate()
            if (field.version != version) return false
        }

        return true
    }

    override fun autoValidateIn(coroutineScope: CoroutineScope) {

        // Validate on value change
        valueFlow
            .drop(1) // Don't validate initial value
            .debounce(DebounceDuration)
            .onEach { validate() }
            .launchIn(coroutineScope)

        // Validate on dependency value change
        snapshotFlow { dependencies.toList() }
            .flatMapLatest { fields ->

                combineTransform(
                    flows = fields.map { (field, _) -> (field as FormFieldImpl).valueFlow },
                    transform = { emit(Unit) },
                )
            }
            .debounce(DebounceDuration)
            .onEach { validate() }
            .launchIn(coroutineScope)
    }
}

private val DebounceDuration = 400.milliseconds

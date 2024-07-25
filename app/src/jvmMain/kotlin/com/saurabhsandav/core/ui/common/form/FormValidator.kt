package com.saurabhsandav.core.ui.common.form

import androidx.compose.runtime.*
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.newChildScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
fun rememberFormValidator(onSubmit: (suspend () -> Unit)? = null): FormValidator {
    val scope = rememberCoroutineScope()
    return remember { FormValidator(scope, onSubmit) }
}

class FormValidator(
    private val coroutineScope: CoroutineScope,
    private val onSubmit: (suspend () -> Unit)? = null,
) {

    private val fields = mutableStateListOf<FormField<*>>()

    private val submitMutex = Mutex()

    val isValid: Boolean by derivedStateOf { fields.all { it.isValid } }

    private var enableSubmit by mutableStateOf(true)

    val canSubmit by derivedStateOf { enableSubmit && isValid }

    fun <T> addField(
        initial: T,
        validation: Validation<T>? = null,
    ): FormField<T> {

        val field = FormFieldImpl(
            initial = initial,
            coroutineScope = coroutineScope.newChildScope(),
            validation = validation
        )

        fields.add(field)

        return field
    }

    fun removeField(field: FormField<*>) {

        if (fields.remove(field)) {
            (field as FormFieldImpl).coroutineScope.cancel()
        }
    }

    suspend fun validate(): Boolean = fields.map { it.validate() }.all { it }

    fun submit() = coroutineScope.launchUnit {

        if (submitMutex.isLocked) return@launchUnit

        submitMutex.withLock {

            enableSubmit = false

            val isValid = validate()

            if (isValid) onSubmit?.let { it() }

            enableSubmit = true
        }
    }
}

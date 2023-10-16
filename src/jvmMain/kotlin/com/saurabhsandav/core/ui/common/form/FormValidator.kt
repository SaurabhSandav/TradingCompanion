package com.saurabhsandav.core.ui.common.form

import androidx.compose.runtime.*
import com.saurabhsandav.core.utils.newChildScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

@Composable
fun rememberFormValidator(): FormValidator {
    val scope = rememberCoroutineScope()
    return remember { FormValidator(scope) }
}

class FormValidator(private val coroutineScope: CoroutineScope) {

    private val fields = mutableStateListOf<FormField<*>>()

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

    val isValid: Boolean by derivedStateOf { fields.all { it.isValid } }

    suspend fun validate(): Boolean = fields.map { it.validate() }.all { it }
}

package com.saurabhsandav.core.ui.common.form

import androidx.compose.runtime.mutableStateListOf
import com.saurabhsandav.core.utils.newChildScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

open class FormModel {

    internal val fields = mutableStateListOf<FormField<*>>()
    private val scopes = mutableMapOf<FormField<*>, CoroutineScope>()

    fun <T> addField(
        initial: T,
        validation: Validation<T>? = null,
    ): FormField<T> {

        val field = FormFieldImpl(
            initial = initial,
            validation = validation,
        )

        fields.add(field)

        return field
    }

    fun removeField(field: FormField<*>) {
        fields.remove(field)
        scopes.remove(field)?.cancel()
    }

    internal fun onAttach(parentScope: CoroutineScope) {

        fields.forEach { field ->
            val fieldScope = parentScope.newChildScope()
            field.autoValidateIn(fieldScope)
            scopes[field] = fieldScope
        }
    }

    internal fun onDetach() {

        fields.forEach { field ->
            scopes.remove(field)?.cancel()
        }
    }
}

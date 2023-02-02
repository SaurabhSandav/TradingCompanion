package com.saurabhsandav.core.ui.common.form

import androidx.compose.runtime.MutableState

interface FormField<T> : MutableState<T> {

    val errorMessage: String?

    val isValid: Boolean

    val validations: Set<Validation<T>>

    fun validate()

    fun registerDependent(state: FormField<*>)

    fun unregisterDependent(state: FormField<*>)
}

inline val FormField<*>.isError: Boolean
    get() = !isValid

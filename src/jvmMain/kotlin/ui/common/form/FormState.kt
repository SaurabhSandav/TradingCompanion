package ui.common.form

import androidx.compose.runtime.Stable

@Stable
interface FormState {

    fun isValid(): Boolean
}

fun interface MutableFieldState<T> {

    fun setValue(newValue: T)
}

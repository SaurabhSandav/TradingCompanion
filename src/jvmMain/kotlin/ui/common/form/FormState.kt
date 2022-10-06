package ui.common.form

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
interface FormState<T> {

    val value: T

    val isError: Boolean

    fun validate()
}

fun interface MutableFieldState<T> {

    fun setValue(newValue: T)
}

abstract class ComposeFormState<T>(
    initial: T,
) : FormState<T> {

    final override var value by mutableStateOf(initial)
        protected set

    final override var isError by mutableStateOf(false)
        protected set
}

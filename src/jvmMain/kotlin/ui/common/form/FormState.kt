package ui.common.form

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
interface FormState {

    fun isValid(): Boolean
}

fun interface MutableFieldState<T> {

    fun setValue(newValue: T)
}

@Stable
class TextFieldState internal constructor(
    initial: String,
    private val isErrorCheck: (String) -> Boolean = { false },
    onValueChange: MutableFieldState<String>.(String) -> Unit = { setValue(it) },
) : FormState {

    var value by mutableStateOf(initial)
        private set

    var isError by mutableStateOf(false)
        private set

    override fun isValid(): Boolean {
        isError = isErrorCheck(value)
        return !isError
    }

    val onValueChange: (String) -> Unit = { newValue ->
        isError = isErrorCheck(value)
        MutableFieldState<String> { value = it }.onValueChange(newValue)
    }
}

@Stable
class SwitchState internal constructor(
    initial: Boolean,
    onCheckedChange: MutableFieldState<Boolean>.(Boolean) -> Unit = { setValue(it) },
) : FormState {

    var value by mutableStateOf(initial)
        private set

    override fun isValid(): Boolean = true

    val onCheckedChange: (Boolean) -> Unit = { newValue ->
        MutableFieldState<Boolean> { value = it }.onCheckedChange(newValue)
    }
}

@Stable
class SingleSelectionState internal constructor(
    private val labelText: String,
    onSelectionChange: MutableFieldState<String>.(String) -> Unit = { setValue(it) },
) : FormState {

    var value by mutableStateOf(labelText)
        private set

    var isError by mutableStateOf(false)
        private set

    override fun isValid(): Boolean {
        isError = value == labelText
        return !isError
    }

    val onSelectionChange: (String) -> Unit = { newValue ->
        isError = newValue == labelText
        MutableFieldState<String> { value = it }.onSelectionChange(newValue)
    }
}

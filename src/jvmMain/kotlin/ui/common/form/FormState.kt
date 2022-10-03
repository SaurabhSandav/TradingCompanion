package ui.common.form

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
sealed class FormState {

    abstract fun isValid(): Boolean
}

@Stable
class TextFieldState(
    initialValue: String,
    private val isErrorCheck: (String) -> Boolean,
    onValueChange: (prev: String, new: String) -> String = { _, new -> new },
) : FormState() {

    var value by mutableStateOf(initialValue)
        private set

    var isError by mutableStateOf(false)
        private set

    override fun isValid(): Boolean {
        isError = isErrorCheck(value)
        return !isError
    }

    val onValueChange: (String) -> Unit = {
        isError = isErrorCheck(it)
        value = onValueChange(value, it)
    }
}

@Stable
class SwitchState(
    initialValue: Boolean,
    onCheckedChange: (new: Boolean) -> Boolean = { new -> new },
) : FormState() {

    var value by mutableStateOf(initialValue)
        private set

    override fun isValid(): Boolean = true

    val onCheckedChange: (Boolean) -> Unit = {
        value = onCheckedChange(it)
    }
}

@Stable
class SingleSelectionState(
    private val labelText: String,
    onSelectionChange: (new: String) -> String = { new -> new },
) : FormState() {

    var value by mutableStateOf(labelText)
        private set

    var isError by mutableStateOf(false)
        private set

    override fun isValid(): Boolean {
        isError = value == labelText
        println(isError)
        return !isError
    }

    val onSelectionChange: (String) -> Unit = {
        isError = it == labelText
        value = onSelectionChange(it)
    }
}

package ui.common.form

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

fun FormManager.textFieldState(
    initial: String,
    isErrorCheck: (String) -> Boolean,
    onValueChange: MutableFieldState<String>.(String) -> Unit = { setValue(it) },
): TextFieldState {

    val saver = Saver<TextFieldState, Any>(
        save = { it.value },
        restore = { TextFieldState(it as String, isErrorCheck, onValueChange) },
    )

    return controlState(saver) { TextFieldState(initial, isErrorCheck, onValueChange) }
}

fun FormManager.switchState(
    initial: Boolean,
    onCheckedChange: MutableFieldState<Boolean>.(Boolean) -> Unit = { setValue(it) },
): SwitchState {

    val saver = Saver<SwitchState, Any>(
        save = { it.value },
        restore = { SwitchState(it as Boolean, onCheckedChange) },
    )

    return controlState(saver) { SwitchState(initial, onCheckedChange) }
}

fun FormManager.singleSelectionState(
    initial: String? = null,
    isRequired: Boolean = true,
    onSelectionChange: MutableFieldState<String>.(String) -> Unit = { setValue(it) },
): SingleSelectionState {

    val saver = listSaver<SingleSelectionState, Any?>(
        save = { listOf(it.value, isRequired) },
        restore = {
            SingleSelectionState(
                initial = it[0] as String?,
                isRequired = it[1] as Boolean,
                onSelectionChange = onSelectionChange,
            )
        },
    )

    return controlState(saver) { SingleSelectionState(initial, isRequired, onSelectionChange) }
}

fun FormManager.dateFieldState(
    initial: LocalDate,
    isErrorCheck: (LocalDate) -> Boolean = { false },
    onValueChange: MutableFieldState<LocalDate>.(LocalDate) -> Unit = { setValue(it) },
): DateFieldState {

    val saver = Saver<DateFieldState, Any>(
        save = { it.value },
        restore = {
            DateFieldState(
                initial = it as LocalDate,
                isErrorCheck = isErrorCheck,
                onValueChange = onValueChange,
            )
        },
    )

    return controlState(saver) { DateFieldState(initial, isErrorCheck, onValueChange) }
}

fun FormManager.timeFieldState(
    initial: LocalTime,
    isErrorCheck: (LocalTime) -> Boolean = { false },
    onValueChange: MutableFieldState<LocalTime>.(LocalTime) -> Unit = { setValue(it) },
): TimeFieldState {

    val saver = Saver<TimeFieldState, Any>(
        save = { it.value },
        restore = {
            TimeFieldState(
                initial = it as LocalTime,
                isErrorCheck = isErrorCheck,
                onValueChange = onValueChange,
            )
        },
    )

    return controlState(saver) { TimeFieldState(initial, isErrorCheck, onValueChange) }
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
        MutableFieldState<String> { value = it }.onValueChange(newValue)
        isError = isErrorCheck(value)
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
    initial: String? = null,
    private val isRequired: Boolean = true,
    onSelectionChange: MutableFieldState<String>.(String) -> Unit = { setValue(it) },
) : FormState {

    var value by mutableStateOf(initial)
        private set

    var isError by mutableStateOf(false)
        private set

    override fun isValid(): Boolean {
        isError = isRequired && value == null
        return !isError
    }

    val onSelectionChange: (String) -> Unit = { newValue ->
        MutableFieldState<String> { value = it }.onSelectionChange(newValue)
        isError = isRequired && value == null
    }
}

@Stable
class DateFieldState internal constructor(
    initial: LocalDate,
    private val isErrorCheck: (LocalDate) -> Boolean = { false },
    onValueChange: MutableFieldState<LocalDate>.(LocalDate) -> Unit = { setValue(it) },
) : FormState {

    var value by mutableStateOf(initial)
        private set

    var isError by mutableStateOf(false)
        private set

    override fun isValid(): Boolean {
        isError = isErrorCheck(value)
        return !isError
    }

    val onValueChange: (LocalDate) -> Unit = { newValue ->
        isError = isErrorCheck(value)
        MutableFieldState<LocalDate> { value = it }.onValueChange(newValue)
    }
}

@Stable
class TimeFieldState internal constructor(
    initial: LocalTime,
    private val isErrorCheck: (LocalTime) -> Boolean = { false },
    onValueChange: MutableFieldState<LocalTime>.(LocalTime) -> Unit = { setValue(it) },
) : FormState {

    var value by mutableStateOf(initial)
        private set

    var isError by mutableStateOf(false)
        private set

    override fun isValid(): Boolean {
        isError = isErrorCheck(value)
        return !isError
    }

    val onValueChange: (LocalTime) -> Unit = { newValue ->
        isError = isErrorCheck(value)
        MutableFieldState<LocalTime> { value = it }.onValueChange(newValue)
    }
}

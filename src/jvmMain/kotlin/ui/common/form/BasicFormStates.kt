package ui.common.form

import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

fun FormScope.textFieldState(
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

fun FormScope.switchState(
    initial: Boolean,
    onCheckedChange: MutableFieldState<Boolean>.(Boolean) -> Unit = { setValue(it) },
): SwitchState {

    val saver = Saver<SwitchState, Any>(
        save = { it.value },
        restore = { SwitchState(it as Boolean, onCheckedChange) },
    )

    return controlState(saver) { SwitchState(initial, onCheckedChange) }
}

fun FormScope.singleSelectionState(
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

fun FormScope.dateFieldState(
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

fun FormScope.timeFieldState(
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

fun FormScope.dateTimeFieldState(
    initial: LocalDateTime,
    isErrorCheck: (LocalDateTime) -> Boolean = { false },
    onValueChange: MutableFieldState<LocalDateTime>.(LocalDateTime) -> Unit = { setValue(it) },
): DateTimeFieldState {

    val saver = Saver<DateTimeFieldState, Any>(
        save = { it.value },
        restore = {
            DateTimeFieldState(
                initial = it as LocalDateTime,
                isErrorCheck = isErrorCheck,
                onValueChange = onValueChange,
            )
        },
    )

    return controlState(saver) { DateTimeFieldState(initial, isErrorCheck, onValueChange) }
}

@Stable
class TextFieldState internal constructor(
    initial: String,
    private val isErrorCheck: (String) -> Boolean = { false },
    onValueChange: MutableFieldState<String>.(String) -> Unit = { setValue(it) },
) : ComposeFormState<String>(initial) {

    override fun validate() {
        isError = isErrorCheck(value)
    }

    val onValueChange: (String) -> Unit = { newValue ->
        MutableFieldState<String> { value = it }.onValueChange(newValue)
        validate()
    }
}

@Stable
class SwitchState internal constructor(
    initial: Boolean,
    onCheckedChange: MutableFieldState<Boolean>.(Boolean) -> Unit = { setValue(it) },
) : ComposeFormState<Boolean>(initial) {

    override fun validate() {}

    val onCheckedChange: (Boolean) -> Unit = { newValue ->
        MutableFieldState<Boolean> { value = it }.onCheckedChange(newValue)
    }
}

@Stable
class SingleSelectionState internal constructor(
    initial: String? = null,
    private val isRequired: Boolean = true,
    onSelectionChange: MutableFieldState<String>.(String) -> Unit = { setValue(it) },
) : ComposeFormState<String?>(initial) {

    override fun validate() {
        isError = isRequired && value == null
    }

    val onSelectionChange: (String) -> Unit = { newValue ->
        MutableFieldState<String> { value = it }.onSelectionChange(newValue)
        validate()
    }
}

@Stable
class DateFieldState internal constructor(
    initial: LocalDate,
    private val isErrorCheck: (LocalDate) -> Boolean = { false },
    onValueChange: MutableFieldState<LocalDate>.(LocalDate) -> Unit = { setValue(it) },
) : ComposeFormState<LocalDate>(initial) {

    override fun validate() {
        isError = isErrorCheck(value)
    }

    val onValueChange: (LocalDate) -> Unit = { newValue ->
        MutableFieldState<LocalDate> { value = it }.onValueChange(newValue)
        validate()
    }
}

@Stable
class TimeFieldState internal constructor(
    initial: LocalTime,
    private val isErrorCheck: (LocalTime) -> Boolean = { false },
    onValueChange: MutableFieldState<LocalTime>.(LocalTime) -> Unit = { setValue(it) },
) : ComposeFormState<LocalTime>(initial) {

    override fun validate() {
        isError = isErrorCheck(value)
    }

    val onValueChange: (LocalTime) -> Unit = { newValue ->
        MutableFieldState<LocalTime> { value = it }.onValueChange(newValue)
        validate()
    }
}

@Stable
class DateTimeFieldState internal constructor(
    initial: LocalDateTime,
    private val isErrorCheck: (LocalDateTime) -> Boolean = { false },
    onValueChange: MutableFieldState<LocalDateTime>.(LocalDateTime) -> Unit = { setValue(it) },
) : ComposeFormState<LocalDateTime>(initial) {

    override fun validate() {
        isError = isErrorCheck(value)
    }

    val onValueChange: (LocalDateTime) -> Unit = { newValue ->
        MutableFieldState<LocalDateTime> { value = it }.onValueChange(newValue)
        validate()
    }
}

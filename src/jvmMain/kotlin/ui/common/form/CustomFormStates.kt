package ui.common.form

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

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

fun FormManager.dateFieldState(
    initial: LocalDate,
    isErrorCheck: (LocalDate) -> Boolean = { false },
    onValueChange: MutableFieldState<LocalDate>.(LocalDate) -> Unit = { setValue(it) },
): DateFieldState {
    return DateFieldState(initial, isErrorCheck, onValueChange).also { customState(it) }
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

fun FormManager.timeFieldState(
    initial: LocalTime,
    isErrorCheck: (LocalTime) -> Boolean = { false },
    onValueChange: MutableFieldState<LocalTime>.(LocalTime) -> Unit = { setValue(it) },
): TimeFieldState {
    return TimeFieldState(initial, isErrorCheck, onValueChange).also { customState(it) }
}

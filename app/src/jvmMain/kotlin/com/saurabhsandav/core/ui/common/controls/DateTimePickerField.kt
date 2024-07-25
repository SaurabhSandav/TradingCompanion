package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.derivedState
import com.saurabhsandav.core.ui.common.state
import kotlinx.datetime.*
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char

@Composable
fun DateTimePickerField(
    value: LocalDateTime,
    onValidValueChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    yearRange: IntRange = DatePickerDefaults.YearRange,
) {

    val dateTimeText by state(value) { value.format(DateTimeFormat) }

    var showDateDialog by state { false }
    var showTimeDialog by state { false }

    OutlinedTextField(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial)
                val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (upEvent != null) showDateDialog = true
            }
        },
        value = dateTimeText,
        onValueChange = {},
        enabled = enabled,
        readOnly = true,
        singleLine = true,
        label = label,
        supportingText = supportingText,
        isError = isError,
    )

    if (showDateDialog) {

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember {
                value.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            },
            yearRange = yearRange,
        )
        val confirmEnabled by derivedState { datePickerState.selectedDateMillis != null }

        DatePickerDialog(
            onDismissRequest = { showDateDialog = false },
            confirmButton = {

                TextButton(
                    onClick = {

                        showDateDialog = false

                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedDateMillis != null) {

                            val dateTime = Instant.fromEpochMilliseconds(selectedDateMillis)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .date
                                .atTime(value.time)

                            onValidValueChange(dateTime)

                            showTimeDialog = true
                        }
                    },
                    enabled = confirmEnabled,
                ) {
                    Text("Next")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = { showDateDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimeDialog) {

        val timePickerState = rememberTimePickerState(
            initialHour = value.hour,
            initialMinute = value.minute,
            is24Hour = true,
        )
        var showingPicker by state { true }

        TimePickerDialog(
            title = when {
                showingPicker -> "Select Time "
                else -> "Enter Time"
            },
            onCancel = { showTimeDialog = false },
            onConfirm = {

                showTimeDialog = false

                val dateTime = value.date.atTime(
                    hour = timePickerState.hour,
                    minute = timePickerState.minute,
                )

                onValidValueChange(dateTime)
            },
            toggle = {

                val hint = when {
                    showingPicker -> "Switch to Text Input"
                    else -> "Switch to Picker Input"
                }

                IconButtonWithTooltip(
                    onClick = { showingPicker = !showingPicker },
                    tooltipText = hint,
                ) {

                    Icon(
                        imageVector = when {
                            showingPicker -> Icons.Outlined.Keyboard
                            else -> Icons.Outlined.Schedule
                        },
                        contentDescription = hint
                    )
                }
            }
        ) {

            when {
                showingPicker -> TimePicker(state = timePickerState)
                else -> TimeInput(state = timePickerState)
            }
        }
    }
}

@Composable
private fun TimePickerDialog(
    title: String = "Select Time",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {

        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                ),
        ) {

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                )

                content()

                Row(
                    modifier = Modifier.height(40.dp).fillMaxWidth(),
                ) {

                    toggle()

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }

                    TextButton(onClick = onConfirm) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

// TODO Remove. Blocked on Swing z-order support
@Composable
fun DateTimeField(
    value: LocalDateTime,
    onValidValueChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
) {

    var dateTimeStr by state(value) { value.format(DateTimeFormatLegacy) }
    var isDateTimeValid by state { true }

    OutlinedTextField(
        modifier = modifier,
        value = dateTimeStr,
        onValueChange = { newValue ->

            val trimmed = newValue.trim().take(14)

            // If still editing, update textfield, signal error
            if (trimmed.isEmpty() || trimmed.length < 14) {
                isDateTimeValid = false
                dateTimeStr = trimmed
                return@OutlinedTextField
            }

            // if input length is valid, try to parse date

            if (trimmed.toLongOrNull() == null) return@OutlinedTextField

            val date = runCatching { LocalDateTime.parse(trimmed, DateTimeFormatLegacy) }

            date.onSuccess { onValidValueChange(it) }
            isDateTimeValid = date.isSuccess
            dateTimeStr = trimmed
        },
        label = label,
        supportingText = supportingText,
        isError = isError || !isDateTimeValid,
        singleLine = true,
        enabled = enabled,
        visualTransformation = {

            var out = ""

            for (i in it.text.indices) {
                out += it.text[i]
                when (i) {
                    in listOf(1, 3) -> out += "/"
                    7 -> out += "  "
                    in listOf(9, 11) -> out += ":"
                }
            }

            TransformedText(
                text = AnnotatedString(out),
                offsetMapping = object : OffsetMapping {

                    override fun originalToTransformed(offset: Int): Int {
                        if (offset <= 1) return offset
                        if (offset <= 3) return offset + 1
                        if (offset <= 7) return offset + 2
                        if (offset <= 9) return offset + 4
                        if (offset <= 11) return offset + 5
                        if (offset <= 13) return offset + 6
                        return 20
                    }

                    override fun transformedToOriginal(offset: Int): Int {
                        if (offset <= 2) return offset
                        if (offset <= 5) return offset - 1
                        if (offset <= 10) return offset - 2
                        if (offset <= 11) return offset - 3
                        if (offset <= 14) return offset - 4
                        if (offset <= 17) return offset - 5
                        if (offset <= 19) return offset - 6
                        return 14
                    }
                }
            )
        }
    )
}

private val DateTimeFormatLegacy = LocalDateTime.Format {
    dayOfMonth()
    monthNumber()
    year()
    hour()
    minute()
    second()
}

private val DateTimeFormat = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_FULL)
    char(' ')
    dayOfMonth()
    chars(", ")
    year()
    chars(" - ")
    hour()
    char(':')
    minute()
}

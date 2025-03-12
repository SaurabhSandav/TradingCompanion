package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.derivedState
import com.saurabhsandav.core.ui.common.onTextFieldClickOrEnter
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
        modifier = modifier.onTextFieldClickOrEnter { showDateDialog = true },
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
                    onClick = { showDateDialog = false },
                ) {
                    Text("Cancel")
                }
            },
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
                        contentDescription = hint,
                    )
                }
            },
        ) {

            when {
                showingPicker -> TimePicker(state = timePickerState)
                else -> TimeInput(state = timePickerState)
            }
        }
    }
}

@Composable
internal fun TimePickerDialog(
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

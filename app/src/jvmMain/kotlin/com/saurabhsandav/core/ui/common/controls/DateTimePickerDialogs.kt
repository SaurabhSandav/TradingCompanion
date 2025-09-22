package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.derivedState
import com.saurabhsandav.core.ui.common.state

@Composable
fun AppDatePickerDialog(
    datePickerState: DatePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {

    AppDatePickerDialog(
        modifier = modifier,
        datePickerState = datePickerState,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        confirmButtonText = "OK",
    )
}

@Composable
fun AppTimePickerDialog(
    timePickerState: TimePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {

    AppTimePickerDialog(
        modifier = modifier,
        timePickerState = timePickerState,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        onSetDate = null,
    )
}

@Composable
fun AppDateTimePickerDialog(
    datePickerState: DatePickerState,
    timePickerState: TimePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {

    var showDateDialog by state { true }
    var showTimeDialog by state { false }

    if (showDateDialog) {

        AppDatePickerDialog(
            datePickerState = datePickerState,
            onDismissRequest = onDismissRequest,
            onConfirm = {
                showDateDialog = false
                showTimeDialog = true
            },
            confirmButtonText = "SET TIME",
        )
    }

    if (showTimeDialog) {

        AppTimePickerDialog(
            timePickerState = timePickerState,
            onDismissRequest = onDismissRequest,
            onConfirm = onConfirm,
            onSetDate = {
                showDateDialog = true
                showTimeDialog = false
            },
        )
    }
}

@Composable
private fun AppDatePickerDialog(
    datePickerState: DatePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmButtonText: String,
    modifier: Modifier = Modifier,
) {

    DatePickerDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {

            val confirmEnabled by derivedState { datePickerState.selectedDateMillis != null }

            TextButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                content = { Text(confirmButtonText) },
            )
        },
        dismissButton = {

            TextButton(
                onClick = onDismissRequest,
                content = { Text("CANCEL") },
            )
        },
        content = { DatePicker(state = datePickerState) },
    )
}

@Composable
private fun AppTimePickerDialog(
    timePickerState: TimePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onSetDate: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {

    var showingPicker by state { true }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {

        Surface(
            modifier = modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                ),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),
                ) {

                    Text(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        text = when {
                            showingPicker -> "Select Time "
                            else -> "Enter Time"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )

                    when {
                        showingPicker -> TimePicker(state = timePickerState)
                        else -> TimeInput(state = timePickerState)
                    }
                }

                FlowRow(
                    modifier = Modifier.padding(bottom = 8.dp, start = 6.dp, end = 6.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {

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

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = onDismissRequest,
                        content = { Text("CANCEL") },
                    )

                    if (onSetDate != null) {

                        TextButton(
                            onClick = onSetDate,
                            content = { Text("SET DATE") },
                        )
                    }

                    TextButton(
                        onClick = onConfirm,
                        content = { Text("OK") },
                    )
                }
            }
        }
    }
}

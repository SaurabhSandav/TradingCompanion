package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDialogDefaults.MinHeightForTimePicker
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
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
    initialDisplayMode: TimePickerDisplayMode = TimePickerDisplayMode.Picker,
) {

    var displayMode by remember { mutableStateOf(initialDisplayMode) }
    val screenHeightDp = LocalWindowInfo.current.containerSize.height

    TimePickerDialog(
        modifier = modifier.heightIn(max = 600.dp),
        onDismissRequest = onDismissRequest,
        title = { TimePickerDialogDefaults.Title(displayMode = displayMode) },
        confirmButton = {

            TextButton(
                onClick = onConfirm,
                content = { Text("OK") },
            )
        },
        dismissButton = {

            when {
                onSetDate != null -> TextButton(
                    onClick = onSetDate,
                    content = { Text("SET DATE") },
                )

                else -> TextButton(
                    onClick = onDismissRequest,
                    content = { Text("CANCEL") },
                )
            }
        },
        modeToggleButton = {

            if (screenHeightDp.dp > MinHeightForTimePicker) {

                TimePickerDialogDefaults.DisplayModeToggle(
                    onDisplayModeChange = {
                        displayMode = when (displayMode) {
                            TimePickerDisplayMode.Picker -> TimePickerDisplayMode.Input
                            else -> TimePickerDisplayMode.Picker
                        }
                    },
                    displayMode = displayMode,
                )
            }
        },
    ) {

        when (displayMode) {
            TimePickerDisplayMode.Picker if screenHeightDp.dp > MinHeightForTimePicker ->
                TimePicker(state = timePickerState)

            else -> TimeInput(state = timePickerState)
        }
    }
}

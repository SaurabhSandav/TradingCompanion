package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.saurabhsandav.core.ui.common.derivedState
import com.saurabhsandav.core.ui.common.state
import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char

@Composable
fun DatePickerField(
    value: LocalDate?,
    onValidValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    format: DateTimeFormat<LocalDate> = DateFormat,
    yearRange: IntRange = DatePickerDefaults.YearRange,
) {

    val dateText by state(value) { value?.format(format) ?: "" }
    var showDialog by state { false }

    OutlinedTextField(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial)
                val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (upEvent != null) showDialog = true
            }
        },
        value = dateText,
        onValueChange = {},
        enabled = enabled,
        readOnly = true,
        singleLine = true,
        label = label,
        supportingText = supportingText,
        isError = isError,
    )

    if (showDialog) {

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember { value?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds() },
            yearRange = yearRange,
        )
        val confirmEnabled by derivedState { datePickerState.selectedDateMillis != null }

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {

                TextButton(
                    onClick = {

                        showDialog = false

                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedDateMillis != null) {

                            val date = Instant.fromEpochMilliseconds(selectedDateMillis)
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                .date

                            onValidValueChange(date)
                        }
                    },
                    enabled = confirmEnabled,
                ) {
                    Text("OK")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private val DateFormat = LocalDate.Format {
    monthName(MonthNames.ENGLISH_FULL)
    char(' ')
    dayOfMonth()
    chars(", ")
    year()
}

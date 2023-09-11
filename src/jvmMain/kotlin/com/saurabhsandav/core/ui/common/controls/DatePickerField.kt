package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.saurabhsandav.core.ui.common.derivedState
import com.saurabhsandav.core.ui.common.state
import kotlinx.datetime.*
import java.time.format.DateTimeFormatter

@Composable
fun DatePickerField(
    value: LocalDate,
    onValidValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    yearRange: IntRange = DatePickerDefaults.YearRange,
) {

    val formatter = remember { DateTimeFormatter.ofPattern(DatePattern) }
    val valueUpdated by rememberUpdatedState(value)
    val dateText by derivedState { formatter.format(valueUpdated.toJavaLocalDate()) }
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
        label = label,
        supportingText = supportingText,
        isError = isError,
    )

    if (showDialog) {

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember { value.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() },
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

private const val DatePattern = "MMMM dd, yyyy"

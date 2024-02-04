package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saurabhsandav.core.ui.common.derivedState
import com.saurabhsandav.core.ui.common.state
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DateRangePickerField(
    from: LocalDate?,
    to: LocalDate?,
    onValidValueChange: (from: LocalDate, to: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    format: String = DatePattern,
    yearRange: IntRange = DatePickerDefaults.YearRange,
) {

    val formatter = remember(format) { DateTimeFormatter.ofPattern(format) }
    val fromUpdated by rememberUpdatedState(from)
    val toUpdated by rememberUpdatedState(to)
    val dateText by derivedState {
        "${
            fromUpdated?.toJavaLocalDate()?.let(formatter::format) ?: ""
        } -> ${toUpdated?.toJavaLocalDate()?.let(formatter::format) ?: ""}"
    }
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

        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = remember { from?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds() },
            initialSelectedEndDateMillis = remember { to?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds() },
            yearRange = yearRange,
        )

        DateRangePickerDialog(
            onCancel = { showDialog = false },
            onConfirm = {

                showDialog = false

                val selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis
                val selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis
                if (selectedStartDateMillis != null && selectedEndDateMillis != null) {

                    val from1 = Instant.fromEpochMilliseconds(selectedStartDateMillis)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date

                    val to1 = Instant.fromEpochMilliseconds(selectedEndDateMillis)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date

                    onValidValueChange(from1, to1)
                }
            },
        ) {

            DateRangePicker(
                modifier = Modifier.weight(1F).size(width = 400.dp, height = 600.dp),
                state = dateRangePickerState,
            )
        }
    }
}

@Composable
private fun DateRangePickerDialog(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
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

                content()

                Row(
                    modifier = Modifier.height(40.dp).fillMaxWidth(),
                ) {

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

private const val DatePattern = "MMMM dd, yyyy"

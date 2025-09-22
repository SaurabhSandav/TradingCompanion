package com.saurabhsandav.core.ui.common.controls

import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.OutlinedTextBox
import com.saurabhsandav.core.ui.common.state
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

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

    var showDateTimePickerDialog by state { false }

    OutlinedTextBox(
        modifier = modifier,
        value = dateTimeText,
        onClick = { showDateTimePickerDialog = true },
        enabled = enabled,
        singleLine = true,
        label = label,
        supportingText = supportingText,
        isError = isError,
    )

    if (showDateTimePickerDialog) {

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember {
                value.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            },
            yearRange = yearRange,
        )

        val timePickerState = rememberTimePickerState(
            initialHour = value.hour,
            initialMinute = value.minute,
            is24Hour = true,
        )

        AppDateTimePickerDialog(
            datePickerState = datePickerState,
            timePickerState = timePickerState,
            onDismissRequest = { showDateTimePickerDialog = false },
            onConfirm = {

                val date = datePickerState.selectedDateMillis!!
                    .let(Instant::fromEpochMilliseconds)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date

                val time = LocalTime(
                    hour = timePickerState.hour,
                    minute = timePickerState.minute,
                )

                val dateTime = date.atTime(time)

                onValidValueChange(dateTime)

                showDateTimePickerDialog = false
            },
        )
    }
}

private val DateTimeFormat = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_FULL)
    char(' ')
    day()
    chars(", ")
    year()
    chars(" - ")
    hour()
    char(':')
    minute()
}

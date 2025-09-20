package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.OutlinedTextBox
import com.saurabhsandav.core.ui.common.state
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun DatePickerField(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    format: DateTimeFormat<LocalDate> = DateFormat,
    yearRange: IntRange = DatePickerDefaults.YearRange,
) {

    val dateText by state(value) { value?.format(format).orEmpty() }
    var showDialog by state { false }

    OutlinedTextBox(
        modifier = modifier,
        value = dateText,
        onClick = { showDialog = true },
        enabled = enabled,
        lineLimits = TextFieldLineLimits.SingleLine,
        label = label,
        supportingText = supportingText,
        isError = isError,
    )

    if (showDialog) {

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember { value?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds() },
            yearRange = yearRange,
        )

        AppDatePickerDialog(
            datePickerState = datePickerState,
            onDismissRequest = { showDialog = false },
            onConfirm = {

                val date = datePickerState.selectedDateMillis!!
                    .let(Instant::fromEpochMilliseconds)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date

                onValueChange(date)

                showDialog = false
            },
        )
    }
}

private val DateFormat = LocalDate.Format {
    monthName(MonthNames.ENGLISH_FULL)
    char(' ')
    day()
    chars(", ")
    year()
}

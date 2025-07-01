package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.controls.DatePickerField
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.form.rememberFormValidator
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.core.ui.common.form.validatedValue
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.DateInterval
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char

@Composable
internal fun DateIntervalFilterItem(
    dateInterval: DateInterval,
    onDateIntervalChange: (DateInterval) -> Unit,
) {

    TradeFilterItem(
        title = "Date Interval",
        expandInitially = dateInterval != DateInterval.All,
    ) {

        Column {

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
                horizontalArrangement = Arrangement.spacedBy(
                    space = MaterialTheme.dimens.rowHorizontalSpacing,
                    alignment = Alignment.End,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowVerticalSpacing),
            ) {

                TradeFilterChip(
                    label = "All",
                    selected = dateInterval == DateInterval.All,
                    onClick = { onDateIntervalChange(DateInterval.All) },
                )

                TradeFilterChip(
                    label = "Today",
                    selected = dateInterval == DateInterval.Today,
                    onClick = { onDateIntervalChange(DateInterval.Today) },
                )

                TradeFilterChip(
                    label = "This Week",
                    selected = dateInterval == DateInterval.ThisWeek,
                    onClick = { onDateIntervalChange(DateInterval.ThisWeek) },
                )

                TradeFilterChip(
                    label = "This Month",
                    selected = dateInterval == DateInterval.ThisMonth,
                    onClick = { onDateIntervalChange(DateInterval.ThisMonth) },
                )

                TradeFilterChip(
                    label = "This Year",
                    selected = dateInterval == DateInterval.ThisYear,
                    onClick = { onDateIntervalChange(DateInterval.ThisYear) },
                )

                TradeFilterChip(
                    label = "Custom",
                    selected = dateInterval is DateInterval.Custom,
                    onClick = { onDateIntervalChange(DateInterval.Custom()) },
                )
            }

            AnimatedVisibility(visible = dateInterval is DateInterval.Custom) {

                val formModel = remember {
                    val custom = dateInterval as DateInterval.Custom
                    DateIntervalFormModel(custom.from, custom.to)
                }
                val validator = rememberFormValidator(listOf(formModel))

                LaunchedEffect(Unit) {

                    // FormFields are not validated on initialization. Force validation.
                    validator.validate()

                    snapshotFlow { formModel.fromField.value to formModel.toField.value }
                        .map { (from, to) -> DateInterval.Custom(from, to) }
                        .collect(onDateIntervalChange)
                }

                CustomForm(formModel)
            }
        }
    }
}

@Composable
private fun CustomForm(formModel: DateIntervalFormModel) {

    Row(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
    ) {

        DatePicker(
            label = "From",
            formField = formModel.fromField,
        )

        DatePicker(
            label = "To",
            formField = formModel.toField,
        )
    }
}

@Composable
private fun RowScope.DatePicker(
    label: String,
    formField: FormField<LocalDate?>,
) {

    Box(modifier = Modifier.weight(1F)) {

        DatePickerField(
            value = formField.value,
            onValidValueChange = { formField.value = it },
            label = { Text(label) },
            format = DateFormat,
            isError = formField.isError,
            supportingText = formField.errorsMessagesAsSupportingText(),
        )

        androidx.compose.animation.AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 17.dp, end = 4.dp),
            visible = formField.value != null,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
        ) {

            // DatePickerField consumes all click events itself.
            // Work around by aligning a separate IconButton on top of DatePickerField.
            IconButton(
                onClick = { formField.value = null },
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
        }
    }
}

private class DateIntervalFormModel(
    from: LocalDate?,
    to: LocalDate?,
) : FormModel() {

    val fromField = addField(from)

    val toField = addField(to) {
        isRequired(false)

        val validatedFrom = fromField.validatedValue()

        if (this != null && validatedFrom != null && validatedFrom > this) {
            reportInvalid("Cannot be less than from")
        }
    }
}

private val DateFormat = LocalDate.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day()
    chars(", ")
    year()
}

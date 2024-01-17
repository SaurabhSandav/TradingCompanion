package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.controls.TimeField
import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.form.rememberFormValidator
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.TimeInterval
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime

@Composable
internal fun TimeIntervalFilterItem(
    timeInterval: TimeInterval,
    onTimeIntervalChange: (TimeInterval) -> Unit,
) {

    TradeFilterItem(
        title = "Time Interval",
        expandInitially = timeInterval != TimeInterval.All,
    ) {

        Column {

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                TradeFilterChip(
                    label = "All",
                    selected = timeInterval == TimeInterval.All,
                    onClick = { onTimeIntervalChange(TimeInterval.All) },
                )

                TradeFilterChip(
                    label = "Custom",
                    selected = timeInterval is TimeInterval.Custom,
                    onClick = { onTimeIntervalChange(TimeInterval.Custom()) },
                )
            }

            AnimatedVisibility(visible = timeInterval is TimeInterval.Custom) {

                val validator = rememberFormValidator()
                val formModel = remember {
                    val custom = timeInterval as TimeInterval.Custom
                    TimeIntervalFormModel(validator, custom.from, custom.to)
                }

                LaunchedEffect(Unit) {

                    // FormFields are not validated on initialization. Force validation.
                    validator.validate()

                    snapshotFlow { formModel.fromField.value to formModel.toField.value }
                        .map { (from, to) -> TimeInterval.Custom(from, to) }
                        .collect(onTimeIntervalChange)
                }

                CustomForm(formModel)
            }
        }
    }
}

@Composable
private fun CustomForm(
    formModel: TimeIntervalFormModel,
) {

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        TimePicker(
            label = "From",
            formField = formModel.fromField,
        )

        TimePicker(
            label = "To",
            formField = formModel.toField,
        )
    }
}

@Composable
private fun RowScope.TimePicker(
    label: String,
    formField: FormField<LocalTime?>,
) {

    TimeField(
        modifier = Modifier.weight(1F),
        value = formField.value,
        onValidValueChange = { formField.value = it },
        label = { Text(label) },
        trailingIcon = {

            androidx.compose.animation.AnimatedVisibility(
                visible = formField.value != null,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
            ) {

                IconButton(onClick = { formField.value = null }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        isError = formField.isError,
        supportingText = formField.errorMessage?.let { { Text(it) } },
    )
}

@Stable
private class TimeIntervalFormModel(
    validator: FormValidator,
    from: LocalTime?,
    to: LocalTime?,
) {

    val fromField = validator.addField(from)

    val toField = validator.addField(to) {

        val validatedFrom = validated(fromField)

        if (this != null && validatedFrom != null) {

            check(
                value = validatedFrom <= this,
                errorMessage = { "Cannot be less than from" },
            )
        }
    }
}

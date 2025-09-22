package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.controls.TimeFieldDefaults
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.FormModel
import com.saurabhsandav.core.ui.common.form.adapter.addMutableStateField
import com.saurabhsandav.core.ui.common.form.finishValidation
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.form.rememberFormValidator
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.core.ui.common.form.validatedValue
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import com.saurabhsandav.core.ui.theme.dimens
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
                modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
                horizontalArrangement = Arrangement.spacedBy(
                    space = MaterialTheme.dimens.rowHorizontalSpacing,
                    alignment = Alignment.End,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowVerticalSpacing),
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

                val formModel = remember {
                    val custom = timeInterval as TimeInterval.Custom
                    TimeIntervalFormModel(custom.from, custom.to)
                }
                val validator = rememberFormValidator(listOf(formModel))

                LaunchedEffect(Unit) {

                    // FormFields are not validated on initialization. Force validation.
                    validator.validate()

                    snapshotFlow { formModel.fromField.value to formModel.toField.value }
                        .map { (from, to) ->
                            TimeInterval.Custom(
                                from = TimeFieldDefaults.parseOrNull(from),
                                to = TimeFieldDefaults.parseOrNull(to),
                            )
                        }
                        .collect(onTimeIntervalChange)
                }

                CustomForm(formModel)
            }
        }
    }
}

@Composable
private fun CustomForm(formModel: TimeIntervalFormModel) {

    Row(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
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
    formField: FormField<MutableState<String>, String>,
) {

    OutlinedTextField(
        modifier = Modifier.weight(1F),
        value = formField.value,
        onValueChange = TimeFieldDefaults.onValueChange { newValue -> formField.holder.value = newValue },
        label = { Text(label) },
        isError = formField.isError,
        supportingText = formField.errorsMessagesAsSupportingText(),
        singleLine = true,
        visualTransformation = TimeFieldDefaults.VisualTransformation,
        trailingIcon = {

            androidx.compose.animation.AnimatedVisibility(
                visible = formField.value.isNotEmpty(),
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
            ) {

                IconButton(onClick = { formField.holder.value = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
    )
}

private class TimeIntervalFormModel(
    from: LocalTime?,
    to: LocalTime?,
) : FormModel() {

    val fromField = addMutableStateField(from?.let(TimeFieldDefaults::format).orEmpty()) {
        isRequired(false)
        with(TimeFieldDefaults) { validate() }
    }

    val toField = addMutableStateField(to?.let(TimeFieldDefaults::format).orEmpty()) {
        isRequired(false)

        val from = fromField.validatedValue()
            .takeIf { it.isNotEmpty() }
            ?.let(TimeFieldDefaults::parse)
        val to = with(TimeFieldDefaults) { validate() } ?: finishValidation()

        if (from != null && from >= to) reportInvalid("Cannot be before or same as from")
    }
}

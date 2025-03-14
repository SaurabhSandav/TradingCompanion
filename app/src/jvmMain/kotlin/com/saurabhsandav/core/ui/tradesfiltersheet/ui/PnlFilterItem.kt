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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.FormField
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.form.rememberFormValidator
import com.saurabhsandav.core.ui.common.form.reportInvalid
import com.saurabhsandav.core.ui.common.form.validatedValue
import com.saurabhsandav.core.ui.common.form.validations.isBigDecimal
import com.saurabhsandav.core.ui.common.form.validations.isRequired
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.PNL
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

@Composable
internal fun PnlFilterItem(
    pnl: PNL,
    onPnlChange: (PNL) -> Unit,
    filterByNetPnl: Boolean,
    onFilterByNetPnlChange: (Boolean) -> Unit,
) {

    TradeFilterItem(
        title = "PNL",
        expandInitially = pnl != PNL.All,
    ) {

        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.containerPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
            ) {

                Text("Filter By")

                Spacer(Modifier.weight(1F))

                SingleChoiceSegmentedButtonRow {

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = { onFilterByNetPnlChange(false) },
                        selected = !filterByNetPnl,
                        label = { Text("PNL") },
                    )

                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = { onFilterByNetPnlChange(true) },
                        selected = filterByNetPnl,
                        // SegmentedButton breaks it into 2 lines. Forcing single line causes weird layout.
                        // Works fine with NBSP.
                        label = { Text("Net\u00A0PNL") },
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = MaterialTheme.dimens.rowHorizontalSpacing,
                    alignment = Alignment.End,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowVerticalSpacing),
            ) {

                TradeFilterChip(
                    label = "All",
                    selected = pnl == PNL.All,
                    onClick = { onPnlChange(PNL.All) },
                )

                TradeFilterChip(
                    label = "Breakeven",
                    selected = pnl == PNL.Breakeven,
                    onClick = { onPnlChange(PNL.Breakeven) },
                )

                TradeFilterChip(
                    label = "Profit",
                    selected = pnl == PNL.Profit,
                    onClick = { onPnlChange(PNL.Profit) },
                )

                TradeFilterChip(
                    label = "Loss",
                    selected = pnl == PNL.Loss,
                    onClick = { onPnlChange(PNL.Loss) },
                )

                TradeFilterChip(
                    label = "Custom",
                    selected = pnl is PNL.Custom,
                    onClick = { onPnlChange(PNL.Custom()) },
                )
            }

            AnimatedVisibility(visible = pnl is PNL.Custom) {

                val validator = rememberFormValidator()
                val formModel = remember {
                    val custom = pnl as PNL.Custom
                    PnlFormModel(validator, custom.from, custom.to)
                }

                LaunchedEffect(Unit) {

                    // FormFields are not validated on initialization. Force validation.
                    validator.validate()

                    snapshotFlow { formModel.fromField.value to formModel.toField.value }
                        .map { (fromStr, toStr) ->

                            val from = fromStr.ifEmpty { null }?.toBigDecimalOrNull()
                            val to = toStr.ifEmpty { null }?.toBigDecimalOrNull()

                            PNL.Custom(from, to)
                        }
                        .collect(onPnlChange)
                }

                CustomForm(formModel)
            }
        }
    }
}

@Composable
private fun CustomForm(formModel: PnlFormModel) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
    ) {

        CustomField(
            label = "From",
            formField = formModel.fromField,
        )

        CustomField(
            label = "To",
            formField = formModel.toField,
        )
    }
}

@Composable
private fun RowScope.CustomField(
    label: String,
    formField: FormField<String>,
) {

    OutlinedTextField(
        modifier = Modifier.weight(1F),
        value = formField.value,
        onValueChange = { formField.value = it.trim() },
        label = { Text(label) },
        trailingIcon = {

            androidx.compose.animation.AnimatedVisibility(
                visible = formField.value.isNotBlank(),
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
            ) {

                IconButton(onClick = { formField.value = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        isError = formField.isError,
        supportingText = formField.errorsMessagesAsSupportingText(),
        singleLine = true,
    )
}

private class PnlFormModel(
    validator: FormValidator,
    from: BigDecimal?,
    to: BigDecimal?,
) {

    val fromField = validator.addField(from?.toPlainString().orEmpty()) {
        isRequired(false)
        isBigDecimal()
    }

    val toField = validator.addField(to?.toPlainString().orEmpty()) {
        isRequired(false)
        isBigDecimal()?.apply {

            val validatedFrom = fromField.validatedValue().toBigDecimalOrNull()

            if (validatedFrom != null && validatedFrom > this) reportInvalid("Cannot be less than from")
        }
    }
}

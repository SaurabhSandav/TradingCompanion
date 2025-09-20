package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableCell.Width
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.content
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.common.thenIf
import com.saurabhsandav.core.ui.trade.TargetPreviewer
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTarget
import com.saurabhsandav.core.ui.trade.ui.AddTargetFormState.ActiveField
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimalOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

@Composable
internal fun TargetsList(
    targets: List<TradeTarget>,
    showRValues: Boolean,
    targetPreviewer: Flow<TargetPreviewer>,
    onAddTarget: (KBigDecimal) -> Unit,
    onDeleteTarget: (KBigDecimal) -> Unit,
    onSetPrimaryTarget: (KBigDecimal) -> Unit,
    modifier: Modifier = Modifier,
) {

    val schema = remember(showRValues) { TargetTableSchema(showRValues) }
    var showForm by state { false }

    TradeSection(
        modifier = modifier,
        title = "Targets",
        subtitle = when {
            targets.isEmpty() -> "No Targets"
            targets.size == 1 -> "1 Target"
            else -> "${targets.size} Targets"
        },
        trailingContent = {

            TradeSectionButton(
                onClick = { showForm = !showForm },
                text = "Add Target",
            )
        },
    ) {

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            AnimatedVisibility(targets.isNotEmpty() || showForm) {

                AnimatedContent(showForm) { showFormT ->

                    Column(
                        modifier = Modifier.thenIf(showForm) {
                            background(TradeSectionDefaults.backgroundLow)
                        },
                    ) {

                        if (showFormT) {

                            AddTargetForm(
                                schema = schema,
                                previewer = targetPreviewer,
                                onAdd = onAddTarget,
                                onDismiss = { showForm = false },
                            )

                            HorizontalDivider()
                        } else {

                            schema.SimpleHeader {
                                this.target.text { "Target" }
                                rValue?.text { "R" }
                                profit.text { "Profit" }
                                netProfit.text { "Net Profit" }
                            }
                        }
                    }
                }
            }

            // Targets list
            targets.forEach { target ->

                key(target) {

                    schema.SimpleRow {
                        this.target.text { target.priceText }
                        rValue?.text { target.rValue }
                        profit.text { target.profit }
                        netProfit.text { target.netProfit }
                        options.content {

                            Row {

                                ToggleIsPrimaryButton(
                                    typeText = "Target",
                                    isPrimary = target.isPrimary,
                                    onToggle = { onSetPrimaryTarget(target.price) },
                                )

                                DeleteIconButton(
                                    deleteTypeText = "Target @ ${target.priceText}",
                                    onDelete = { onDeleteTarget(target.price) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTargetForm(
    schema: TargetTableSchema,
    previewer: Flow<TargetPreviewer>,
    onAdd: (KBigDecimal) -> Unit,
    onDismiss: () -> Unit,
) {

    val coroutineScope = rememberCoroutineScope()
    val formState = remember(coroutineScope, previewer, onAdd) {

        AddTargetFormState(coroutineScope, previewer) {
            onAdd(it)
            onDismiss()
        }
    }

    val textFieldModifier = Modifier.fillMaxWidth().onKeyEvent {

        when (it.key) {
            Key.Enter, Key.NumPadEnter -> formState.submit()
            Key.Escape -> onDismiss()
            else -> return@onKeyEvent false
        }

        true
    }

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = TradeSectionDefaults.backgroundLow,
        unfocusedContainerColor = TradeSectionDefaults.backgroundLow,
        errorContainerColor = TradeSectionDefaults.backgroundLow,
    )

    schema.SimpleRow {

        target.content {

            val focusRequester = remember { FocusRequester() }

            TextField(
                modifier = textFieldModifier.focusRequester(focusRequester),
                state = formState.price,
                inputTransformation = { formState.activeField = ActiveField.Price },
                isError = formState.priceIsError,
                lineLimits = TextFieldLineLimits.SingleLine,
                colors = textFieldColors,
                placeholder = {

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Target",
                    )
                },
            )

            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }
        }
        rValue?.content {

            TextField(
                modifier = textFieldModifier,
                state = formState.rValue,
                inputTransformation = { formState.activeField = ActiveField.RValue },
                isError = formState.rValueIsError,
                lineLimits = TextFieldLineLimits.SingleLine,
                colors = textFieldColors,
                placeholder = {

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "R",
                    )
                },
                outputTransformation = { if (toString().isNotEmpty()) append(" R") },
            )
        }
        profit.content {

            TextField(
                modifier = textFieldModifier,
                state = formState.profit,
                inputTransformation = { formState.activeField = ActiveField.Profit },
                isError = formState.profitIsError,
                lineLimits = TextFieldLineLimits.SingleLine,
                colors = textFieldColors,
                placeholder = {

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Profit",
                    )
                },
            )
        }
        netProfit.content {

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = formState.netProfit,
            )
        }
        options.content {

            IconButtonWithTooltip(
                onClick = onDismiss,
                tooltipText = "Cancel",
                content = { Icon(Icons.Default.Close, contentDescription = "Cancel") },
            )
        }
    }
}

private class AddTargetFormState(
    coroutineScope: CoroutineScope,
    previewerFlow: Flow<TargetPreviewer>,
    private val onAdd: (KBigDecimal) -> Unit,
) {

    private var finalPrice: KBigDecimal? = null

    val price = TextFieldState()
    var priceIsError by mutableStateOf(false)
        private set

    val rValue = TextFieldState()
    var rValueIsError by mutableStateOf(false)
        private set

    val profit = TextFieldState()
    var profitIsError by mutableStateOf(false)
        private set

    var netProfit by mutableStateOf("")
        private set

    var activeField = ActiveField.Price

    init {

        combine(
            previewerFlow,
            snapshotFlow { Triple(price.text, rValue.text, profit.text) },
        ) { previewer, (priceText, rValueText, profitText) ->

            val priceBD = priceText.toString().toKBigDecimalOrNull()
            val rValueBD = rValueText.toString().toKBigDecimalOrNull()
            val profitBD = profitText.toString().toKBigDecimalOrNull()

            val target = when (activeField) {
                ActiveField.Price -> priceBD?.let(previewer::atPrice)
                ActiveField.RValue -> rValueBD?.let(previewer::atRValue)
                ActiveField.Profit -> profitBD?.let(previewer::atProfit)
            }

            finalPrice = target?.price

            when (activeField) {
                ActiveField.Price -> {
                    rValue.setTextAndPlaceCursorAtEnd(target?.rValue.orEmpty())
                    profit.setTextAndPlaceCursorAtEnd(target?.profit.orEmpty())
                }

                ActiveField.RValue -> {
                    price.setTextAndPlaceCursorAtEnd(target?.priceText.orEmpty())
                    profit.setTextAndPlaceCursorAtEnd(target?.profit.orEmpty())
                }

                ActiveField.Profit -> {
                    price.setTextAndPlaceCursorAtEnd(target?.priceText.orEmpty())
                    rValue.setTextAndPlaceCursorAtEnd(target?.rValue.orEmpty())
                }
            }

            netProfit = target?.netProfit.orEmpty()
            priceIsError = activeField == ActiveField.Price && target == null
            rValueIsError = activeField == ActiveField.RValue && target == null
            profitIsError = activeField == ActiveField.Profit && target == null
        }.launchIn(coroutineScope)
    }

    fun submit() {
        finalPrice?.let(onAdd)
    }

    enum class ActiveField {
        Price,
        RValue,
        Profit,
    }
}

private class TargetTableSchema(
    showRValues: Boolean,
) : TableSchema() {

    val target = cell()
    val rValue = if (showRValues) cell() else null
    val profit = cell()
    val netProfit = cell()
    val options = cell(
        width = Width.Fixed(StopTargetOptionsWidth),
        contentAlignment = Alignment.CenterEnd,
    )
}

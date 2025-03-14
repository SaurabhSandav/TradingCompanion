package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import java.math.BigDecimal

@Composable
internal fun TargetsList(
    targets: List<TradeTarget>,
    showRValues: Boolean,
    targetPreviewer: Flow<TargetPreviewer>,
    onAddTarget: (BigDecimal) -> Unit,
    onDeleteTarget: (BigDecimal) -> Unit,
    onSetPrimaryTarget: (BigDecimal) -> Unit,
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
    onAdd: (BigDecimal) -> Unit,
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
                value = formState.price,
                onValueChange = formState::onPriceChange,
                isError = formState.priceIsError,
                singleLine = true,
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
                value = formState.rValue,
                onValueChange = formState::onRValueChange,
                isError = formState.rValueIsError,
                singleLine = true,
                colors = textFieldColors,
                placeholder = {

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "R",
                    )
                },
                visualTransformation = rValueVisualTransformation,
            )
        }
        profit.content {

            TextField(
                modifier = textFieldModifier,
                value = formState.profit,
                onValueChange = formState::onProfitChange,
                isError = formState.profitIsError,
                singleLine = true,
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
    private val onAdd: (BigDecimal) -> Unit,
) {

    private val changeEvents = MutableSharedFlow<ChangeEvent>(replay = 1)

    private var finalPrice: BigDecimal? = null

    var price by mutableStateOf("")
        private set
    var priceIsError by mutableStateOf(false)
        private set

    var rValue by mutableStateOf("")
        private set
    var rValueIsError by mutableStateOf(false)
        private set

    var profit by mutableStateOf("")
        private set
    var profitIsError by mutableStateOf(false)
        private set

    var netProfit by mutableStateOf("")
        private set

    init {

        combine(previewerFlow, changeEvents) { previewer, changeEvent ->

            val target = when (changeEvent) {
                is ChangeEvent.Price -> changeEvent.value?.let(previewer::atPrice)
                is ChangeEvent.RValue -> changeEvent.value?.let(previewer::atRValue)
                is ChangeEvent.Profit -> changeEvent.value?.let(previewer::atProfit)
            }

            finalPrice = target?.price
            price = when (changeEvent) {
                is ChangeEvent.Price -> price
                else -> target?.priceText.orEmpty()
            }
            rValue = when (changeEvent) {
                is ChangeEvent.RValue -> rValue
                else -> target?.rValue.orEmpty()
            }
            profit = when (changeEvent) {
                is ChangeEvent.Profit -> profit
                else -> target?.profit.orEmpty()
            }
            netProfit = target?.netProfit.orEmpty()
            priceIsError = changeEvent is ChangeEvent.Price && target == null
            rValueIsError = changeEvent is ChangeEvent.RValue && target == null
            profitIsError = changeEvent is ChangeEvent.Profit && target == null
        }.launchIn(coroutineScope)
    }

    fun onPriceChange(newValue: String) {

        price = newValue.trim()

        changeEvents.tryEmit(ChangeEvent.Price(price.toBigDecimalOrNull()))
    }

    fun onRValueChange(newValue: String) {

        rValue = newValue.trim()

        changeEvents.tryEmit(ChangeEvent.RValue(rValue.toBigDecimalOrNull()))
    }

    fun onProfitChange(newValue: String) {

        profit = newValue.trim()

        changeEvents.tryEmit(ChangeEvent.Profit(profit.toBigDecimalOrNull()))
    }

    fun submit() {
        finalPrice?.let(onAdd)
    }

    private sealed class ChangeEvent {

        data class Price(
            val value: BigDecimal?,
        ) : ChangeEvent()

        data class RValue(
            val value: BigDecimal?,
        ) : ChangeEvent()

        data class Profit(
            val value: BigDecimal?,
        ) : ChangeEvent()
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

private val rValueVisualTransformation = VisualTransformation { text ->

    TransformedText(
        text = if (text.isEmpty()) text else AnnotatedString("${text.text} R"),
        offsetMapping = object : OffsetMapping {

            override fun originalToTransformed(offset: Int): Int = offset

            override fun transformedToOriginal(offset: Int): Int {
                return if (offset >= text.length) text.length else offset
            }
        },
    )
}

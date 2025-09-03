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
import com.saurabhsandav.core.ui.trade.StopPreviewer
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeStop
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.toKBigDecimalOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

@Composable
internal fun StopsList(
    stops: List<TradeStop>,
    stopPreviewer: Flow<StopPreviewer>,
    onAddStop: (KBigDecimal) -> Unit,
    onDeleteStop: (KBigDecimal) -> Unit,
    onSetPrimaryStop: (KBigDecimal) -> Unit,
    modifier: Modifier = Modifier,
) {

    var showForm by state { false }

    TradeSection(
        modifier = modifier,
        title = "Stops",
        subtitle = when {
            stops.isEmpty() -> "No Stops"
            stops.size == 1 -> "1 Stop"
            else -> "${stops.size} Stops"
        },
        trailingContent = {

            TradeSectionButton(
                onClick = { showForm = !showForm },
                text = "Add Stop",
            )
        },
    ) {

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            AnimatedVisibility(stops.isNotEmpty() || showForm) {

                AnimatedContent(showForm) { showFormT ->

                    Column(
                        modifier = Modifier.thenIf(showForm) {
                            background(TradeSectionDefaults.backgroundLow)
                        },
                    ) {

                        if (showFormT) {

                            AddStopForm(
                                previewer = stopPreviewer,
                                onAdd = onAddStop,
                                onDismiss = { showForm = false },
                            )

                            HorizontalDivider()
                        } else {

                            StopTableSchema.SimpleHeader {
                                stop.text { "Stop" }
                                risk.text { "Risk" }
                                netRisk.text { "Net Risk" }
                            }
                        }
                    }
                }
            }

            // Stops list
            stops.forEach { stop ->

                key(stop) {

                    StopTableSchema.SimpleRow {
                        this.stop.text { stop.priceText }
                        risk.text { stop.risk }
                        netRisk.text { stop.netRisk }
                        options.content {

                            Row {

                                ToggleIsPrimaryButton(
                                    typeText = "Stop",
                                    isPrimary = stop.isPrimary,
                                    onToggle = { onSetPrimaryStop(stop.price) },
                                )

                                DeleteIconButton(
                                    deleteTypeText = "Stop @ ${stop.priceText}",
                                    onDelete = { onDeleteStop(stop.price) },
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
private fun AddStopForm(
    previewer: Flow<StopPreviewer>,
    onAdd: (KBigDecimal) -> Unit,
    onDismiss: () -> Unit,
) {

    val coroutineScope = rememberCoroutineScope()
    val formState = remember(coroutineScope, previewer, onAdd) {

        AddStopFormState(coroutineScope, previewer) {
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

    StopTableSchema.SimpleRow {

        stop.content {

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
                        text = "Stop",
                    )
                },
            )

            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }
        }
        risk.content {

            TextField(
                modifier = textFieldModifier,
                value = formState.risk,
                onValueChange = formState::onRiskChange,
                isError = formState.riskIsError,
                singleLine = true,
                colors = textFieldColors,
                placeholder = {

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Risk",
                    )
                },
                visualTransformation = riskVisualTransformation,
            )
        }
        netRisk.content {

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = formState.netRisk,
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

private class AddStopFormState(
    coroutineScope: CoroutineScope,
    previewerFlow: Flow<StopPreviewer>,
    private val onAdd: (KBigDecimal) -> Unit,
) {

    private val changeEvents = MutableSharedFlow<ChangeEvent>(replay = 1)

    private var finalPrice: KBigDecimal? = null

    var price by mutableStateOf("")
        private set
    var priceIsError by mutableStateOf(false)
        private set

    var risk by mutableStateOf("")
        private set
    var riskIsError by mutableStateOf(false)
        private set

    var netRisk by mutableStateOf("")
        private set

    init {

        combine(previewerFlow, changeEvents) { previewer, changeEvent ->

            val stop = when (changeEvent) {
                is ChangeEvent.Price -> changeEvent.value?.let(previewer::atPrice)
                is ChangeEvent.Risk -> changeEvent.value?.let(previewer::atRisk)
            }

            finalPrice = stop?.price
            price = when (changeEvent) {
                is ChangeEvent.Price -> price
                is ChangeEvent.Risk -> stop?.priceText.orEmpty()
            }
            risk = when (changeEvent) {
                is ChangeEvent.Price -> stop?.risk.orEmpty()
                is ChangeEvent.Risk -> risk
            }
            netRisk = stop?.netRisk.orEmpty()
            priceIsError = changeEvent is ChangeEvent.Price && stop == null
            riskIsError = changeEvent is ChangeEvent.Risk && stop == null
        }.launchIn(coroutineScope)
    }

    fun onPriceChange(newValue: String) {

        price = newValue.trim()

        changeEvents.tryEmit(ChangeEvent.Price(price.toKBigDecimalOrNull()))
    }

    fun onRiskChange(newValue: String) {

        risk = newValue.trim()

        changeEvents.tryEmit(ChangeEvent.Risk(risk.toKBigDecimalOrNull()))
    }

    fun submit() {
        finalPrice?.let(onAdd)
    }

    private sealed class ChangeEvent {

        data class Price(
            val value: KBigDecimal?,
        ) : ChangeEvent()

        data class Risk(
            val value: KBigDecimal?,
        ) : ChangeEvent()
    }
}

private object StopTableSchema : TableSchema() {

    val stop = cell()
    val risk = cell()
    val netRisk = cell()
    val options = cell(
        width = Width.Fixed(StopTargetOptionsWidth),
        contentAlignment = Alignment.CenterEnd,
    )
}

private val riskVisualTransformation = VisualTransformation { text ->

    TransformedText(
        text = if (text.isEmpty()) text else AnnotatedString("-${text.text}"),
        offsetMapping = object : OffsetMapping {

            override fun originalToTransformed(offset: Int): Int {
                return if (text.isEmpty()) 0 else offset + 1
            }

            override fun transformedToOriginal(offset: Int) = when {
                text.isEmpty() -> 0
                offset == 0 -> 0
                else -> offset - 1
            }
        },
    )
}

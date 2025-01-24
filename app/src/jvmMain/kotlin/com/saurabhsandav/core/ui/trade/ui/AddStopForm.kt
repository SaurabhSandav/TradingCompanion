package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableCell.Width
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trade.StopPreviewer
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import java.math.BigDecimal

@Composable
internal fun StopsList(
    stops: List<TradeStop>,
    stopPreviewer: Flow<StopPreviewer>,
    onAddStop: (BigDecimal) -> Unit,
    onDeleteStop: (BigDecimal) -> Unit,
    onSetPrimaryStop: (BigDecimal) -> Unit,
    modifier: Modifier,
) {

    Column(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            // Header
            StopTableSchema.SimpleHeader {
                stop.text { "Stop" }
                risk.text { "Risk" }
                netRisk.text { "Net Risk" }
            }

            HorizontalDivider()

            // Stops list
            stops.forEach { stop ->

                key(stop) {

                    StopTableSchema.SimpleRow {
                        this.stop.text { stop.priceText }
                        risk.text { stop.risk }
                        netRisk.text { stop.netRisk }
                        options {

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

            HorizontalDivider()

            // Add Stop Form
            AddStopForm(
                previewer = stopPreviewer,
                onAdd = onAddStop,
            )
        }
    }
}

@Composable
private fun AddStopForm(
    previewer: Flow<StopPreviewer>,
    onAdd: (BigDecimal) -> Unit,
) {

    var showForm by state { false }

    AnimatedContent(
        targetState = showForm,
    ) { showFormT ->

        when {
            showFormT -> {

                val coroutineScope = rememberCoroutineScope()
                val formState = remember(coroutineScope, previewer, onAdd) {

                    AddStopFormState(coroutineScope, previewer) {
                        onAdd(it)
                        showForm = false
                    }
                }

                Row(
                    modifier = Modifier.padding(MaterialTheme.dimens.listItemPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    val textFieldModifier = Modifier.fillMaxWidth().onKeyEvent {

                        when (it.key) {
                            Key.Enter, Key.NumPadEnter -> formState.submit()
                            Key.Escape -> showForm = false
                            else -> return@onKeyEvent false
                        }

                        true
                    }
                    val textFieldColors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        errorContainerColor = MaterialTheme.colorScheme.background,
                    )

                    StopTableSchema.SimpleRow {

                        stop {

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
                        risk {

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
                        netRisk {

                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = formState.netRisk,
                            )
                        }
                        options {

                            IconButtonWithTooltip(
                                onClick = { showForm = false },
                                tooltipText = "Cancel",
                                content = { Icon(Icons.Default.Close, contentDescription = "Cancel") },
                            )
                        }
                    }
                }
            }

            else -> {

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showForm = true },
                    shape = RectangleShape,
                    content = { Text("Add Stop") },
                )
            }
        }
    }
}

private class AddStopFormState(
    coroutineScope: CoroutineScope,
    previewerFlow: Flow<StopPreviewer>,
    private val onAdd: (BigDecimal) -> Unit,
) {

    private val changeEvents = MutableSharedFlow<ChangeEvent>(replay = 1)

    private var finalPrice: BigDecimal? = null

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

        changeEvents.tryEmit(ChangeEvent.Price(price.toBigDecimalOrNull()))
    }

    fun onRiskChange(newValue: String) {

        risk = newValue.trim()

        changeEvents.tryEmit(ChangeEvent.Risk(risk.toBigDecimalOrNull()))
    }

    fun submit() {
        finalPrice?.let(onAdd)
    }

    private sealed class ChangeEvent {

        data class Price(val value: BigDecimal?) : ChangeEvent()

        data class Risk(val value: BigDecimal?) : ChangeEvent()
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

            override fun transformedToOriginal(offset: Int): Int {
                return if (offset == 0) 0 else offset - 1
            }
        },
    )
}

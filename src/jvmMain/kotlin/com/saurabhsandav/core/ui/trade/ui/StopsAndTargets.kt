package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircleOutline
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableCell.Width
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeStop
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
internal fun StopsAndTargets(
    stops: List<TradeStop>,
    previewStop: (BigDecimal) -> Flow<TradeStop?>,
    onAddStop: (BigDecimal) -> Unit,
    onDeleteStop: (BigDecimal) -> Unit,
    onSetPrimaryStop: (BigDecimal) -> Unit,
    targets: List<TradeTarget>,
    previewTarget: (BigDecimal) -> Flow<TradeTarget?>,
    onAddTarget: (BigDecimal) -> Unit,
    onDeleteTarget: (BigDecimal) -> Unit,
    onSetPrimaryTarget: (BigDecimal) -> Unit,
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
    ) {

        StopsList(
            modifier = Modifier.weight(1F),
            stops = stops,
            previewStop = previewStop,
            onAddStop = onAddStop,
            onDeleteStop = onDeleteStop,
            onSetPrimaryStop = onSetPrimaryStop,
        )

        TargetsList(
            modifier = Modifier.weight(1F),
            targets = targets,
            previewTarget = previewTarget,
            onAddTarget = onAddTarget,
            onDeleteTarget = onDeleteTarget,
            onSetPrimaryTarget = onSetPrimaryTarget,
        )
    }
}

@Composable
private fun StopsList(
    stops: List<TradeStop>,
    previewStop: (BigDecimal) -> Flow<TradeStop?>,
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

            val addValueState = remember {
                AddValueState(
                    preview = previewStop,
                    onAdd = onAddStop,
                )
            }

            // Add Stop Form
            AddValueForm(
                addValueState = addValueState,
                valueLabel = "Stop",
                previewContent = { tradeStop ->

                    Text(
                        modifier = Modifier.weight(1F),
                        text = tradeStop.risk,
                    )

                    Text(
                        modifier = Modifier.weight(1F),
                        text = tradeStop.netRisk,
                    )
                },
            )
        }
    }
}

@Composable
private fun TargetsList(
    targets: List<TradeTarget>,
    previewTarget: (BigDecimal) -> Flow<TradeTarget?>,
    onAddTarget: (BigDecimal) -> Unit,
    onDeleteTarget: (BigDecimal) -> Unit,
    onSetPrimaryTarget: (BigDecimal) -> Unit,
    modifier: Modifier,
) {

    Column(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            // Header
            TargetTableSchema.SimpleHeader {
                this.target.text { "Target" }
                profit.text { "Profit" }
                netProfit.text { "Net Profit" }
            }

            HorizontalDivider()

            // Targets list
            targets.forEach { target ->

                key(target) {

                    TargetTableSchema.SimpleRow {
                        this.target.text { target.priceText }
                        profit.text { target.profit }
                        netProfit.text { target.netProfit }
                        options {

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

            HorizontalDivider()

            val addValueState = remember {
                AddValueState(
                    preview = previewTarget,
                    onAdd = onAddTarget,
                )
            }

            // Add Target Form
            AddValueForm(
                addValueState = addValueState,
                valueLabel = "Target",
                previewContent = { tradeTarget ->

                    Text(
                        modifier = Modifier.weight(1F),
                        text = tradeTarget.profit,
                    )

                    Text(
                        modifier = Modifier.weight(1F),
                        text = tradeTarget.netProfit,
                    )
                },
            )
        }
    }
}

@Composable
private fun DeleteIconButton(
    deleteTypeText: String,
    onDelete: () -> Unit,
) {

    var showDeleteConfirmationDialog by state { false }

    IconButtonWithTooltip(
        onClick = { showDeleteConfirmationDialog = true },
        tooltipText = "Delete $deleteTypeText",
        content = {
            Icon(Icons.Default.Close, contentDescription = "Delete $deleteTypeText")
        },
    )

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            subject = deleteTypeText,
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDelete,
        )
    }
}

@Composable
private fun ToggleIsPrimaryButton(
    typeText: String,
    isPrimary: Boolean,
    onToggle: () -> Unit,
) {

    IconButtonWithTooltip(
        onClick = onToggle,
        enabled = !isPrimary,
        tooltipText = when {
            isPrimary -> "Primary $typeText"
            else -> "Set Primary"
        },
        content = {

            Icon(
                imageVector = when {
                    isPrimary -> Icons.Filled.CheckCircle
                    else -> Icons.Outlined.CheckCircleOutline
                },
                contentDescription = when {
                    isPrimary -> "Primary $typeText"
                    else -> "Secondary $typeText"
                },
            )
        },
    )
}

@Composable
private fun <T : Any> AddValueForm(
    addValueState: AddValueState<T>,
    valueLabel: String,
    previewContent: @Composable RowScope.(T) -> Unit,
) {

    AnimatedContent(
        targetState = addValueState.addFormShown,
    ) { showAddForm ->

        when {
            showAddForm -> {

                Row(
                    modifier = Modifier.padding(MaterialTheme.dimens.listItemPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    AddValueTextField(
                        modifier = Modifier.weight(1F),
                        addValueState = addValueState,
                    )

                    val previewValue = addValueState.previewValue.collectAsState(null).value

                    when {
                        addValueState.priceText.isEmpty() || addValueState.isError -> Spacer(Modifier.weight(2F))
                        previewValue == null -> {

                            Text(
                                modifier = Modifier.weight(2F),
                                text = "Invalid $valueLabel",
                            )
                        }

                        else -> previewContent(previewValue)
                    }

                    Box(
                        modifier = Modifier.width(OptionsWidth),
                        contentAlignment = Alignment.CenterEnd,
                    ) {

                        IconButtonWithTooltip(
                            onClick = addValueState::hideAddForm,
                            tooltipText = "Cancel",
                            content = { Icon(Icons.Default.Close, contentDescription = "Cancel") },
                        )
                    }
                }
            }

            else -> {

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = addValueState::showAddForm,
                    shape = RectangleShape,
                    content = { Text("Add $valueLabel") },
                )
            }
        }
    }
}

@Composable
private fun AddValueTextField(
    modifier: Modifier,
    addValueState: AddValueState<*>,
) {

    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    TextField(
        modifier = modifier.focusRequester(focusRequester).onKeyEvent {
            when (it.key) {
                Key.Enter, Key.NumPadEnter -> {
                    coroutineScope.launch {
                        addValueState.submit()
                    }
                    true
                }

                Key.Escape -> {
                    addValueState.hideAddForm()
                    true
                }

                else -> false
            }
        },
        value = addValueState.priceText,
        onValueChange = addValueState::onValueChange,
        isError = addValueState.isError,
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            errorContainerColor = MaterialTheme.colorScheme.background,
        ),
    )

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

private class AddValueState<T : Any>(
    private val preview: (BigDecimal) -> Flow<T?>,
    private val onAdd: (BigDecimal) -> Unit,
) {

    var addFormShown by mutableStateOf(false)
    var priceText by mutableStateOf("")
    var isError by mutableStateOf(false)

    val previewValue = snapshotFlow { priceText }.flatMapLatest {
        it.toBigDecimalOrNull()?.let(preview) ?: emptyFlow()
    }

    fun showAddForm() {
        addFormShown = true
    }

    fun hideAddForm() {
        addFormShown = false
        priceText = ""
        isError = false
    }

    fun onValueChange(newValue: String) {
        priceText = newValue.trim()
        val price = priceText.toBigDecimalOrNull()
        isError = price == null
    }

    suspend fun submit() {

        val price = priceText.toBigDecimalOrNull() ?: return

        // If preview available, price is valid
        preview(price).first() ?: return

        onAdd(price)
        hideAddForm()
    }
}

private object StopTableSchema : TableSchema() {

    val stop = cell()
    val risk = cell()
    val netRisk = cell()
    val options = cell(
        width = Width.Fixed(OptionsWidth),
        contentAlignment = Alignment.CenterEnd,
    )
}

private object TargetTableSchema : TableSchema() {

    val target = cell()
    val profit = cell()
    val netProfit = cell()
    val options = cell(
        width = Width.Fixed(OptionsWidth),
        contentAlignment = Alignment.CenterEnd,
    )
}

private val OptionsWidth = 100.dp

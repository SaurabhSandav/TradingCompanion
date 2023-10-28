package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.Column.Width.Weight
import com.saurabhsandav.core.ui.common.table.DefaultTableHeader
import com.saurabhsandav.core.ui.common.table.DefaultTableRow
import com.saurabhsandav.core.ui.common.table.addColumnText
import com.saurabhsandav.core.ui.common.table.rememberTableSchema
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeStop
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTarget
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
internal fun StopsAndTargets(
    stops: ImmutableList<TradeStop>,
    previewStop: (BigDecimal) -> Flow<TradeStop?>,
    onAddStop: (BigDecimal) -> Unit,
    onDeleteStop: (BigDecimal) -> Unit,
    targets: ImmutableList<TradeTarget>,
    previewTarget: (BigDecimal) -> Flow<TradeTarget?>,
    onAddTarget: (BigDecimal) -> Unit,
    onDeleteTarget: (BigDecimal) -> Unit,
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        StopsList(
            modifier = Modifier.weight(1F),
            stops = stops,
            previewStop = previewStop,
            onAddStop = onAddStop,
            onDeleteStop = onDeleteStop,
        )

        TargetsList(
            modifier = Modifier.weight(1F),
            targets = targets,
            previewTarget = previewTarget,
            onAddTarget = onAddTarget,
            onDeleteTarget = onDeleteTarget,
        )
    }
}

@Composable
private fun StopsList(
    stops: ImmutableList<TradeStop>,
    previewStop: (BigDecimal) -> Flow<TradeStop?>,
    onAddStop: (BigDecimal) -> Unit,
    onDeleteStop: (BigDecimal) -> Unit,
    modifier: Modifier,
) {

    Column(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        val schema = rememberTableSchema<TradeStop> {
            addColumnText("Stop") { it.priceText }
            addColumnText("Risk") { it.risk }
            addColumnText("Net Risk") { it.netRisk }
            addColumn(width = Weight(.5F)) { stop ->

                DeleteIconButton(
                    deleteTypeText = "Stop @ ${stop.priceText}",
                    onDelete = { onDeleteStop(stop.price) },
                )
            }
        }

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            // Header
            DefaultTableHeader(schema = schema)

            Divider()

            // Stops list
            stops.forEach { stop ->

                key(stop) {

                    DefaultTableRow(
                        item = stop,
                        schema = schema,
                    )
                }
            }

            Divider()

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
    targets: ImmutableList<TradeTarget>,
    previewTarget: (BigDecimal) -> Flow<TradeTarget?>,
    onAddTarget: (BigDecimal) -> Unit,
    onDeleteTarget: (BigDecimal) -> Unit,
    modifier: Modifier,
) {

    Column(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        val schema = rememberTableSchema<TradeTarget> {
            addColumnText("Target") { it.priceText }
            addColumnText("Profit") { it.profit }
            addColumnText("Net Profit") { it.netProfit }
            addColumn(width = Weight(.5F)) { target ->

                DeleteIconButton(
                    deleteTypeText = "Target @ ${target.priceText}",
                    onDelete = { onDeleteTarget(target.price) },
                )
            }
        }

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            // Header
            DefaultTableHeader(schema = schema)

            Divider()

            // Targets list
            targets.forEach { target ->

                key(target) {

                    DefaultTableRow(
                        item = target,
                        schema = schema,
                    )
                }
            }

            Divider()

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

    IconButton(
        onClick = { showDeleteConfirmationDialog = true },
    ) {

        Icon(Icons.Default.Close, contentDescription = "Delete $deleteTypeText")
    }

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            deleteTypeText = deleteTypeText,
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDelete,
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    deleteTypeText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        text = {
            Text("Are you sure you want to delete the $deleteTypeText?")
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
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
                    modifier = Modifier.padding(8.dp),
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

                    Box(Modifier.weight(.5F)) {

                        IconButton(
                            onClick = addValueState::hideAddForm,
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

@Stable
private class AddValueState<T : Any>(
    private val preview: (BigDecimal) -> Flow<T?>,
    private val onAdd: (BigDecimal) -> Unit,
) {

    var addFormShown by mutableStateOf(false)
    var priceText by mutableStateOf("")
    var isError by mutableStateOf(false)

    val previewValue = flow {
        emitAll(
            snapshotFlow { priceText }.flatMapLatest { it.toBigDecimalOrNull()?.let(preview) ?: emptyFlow() }
        )
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

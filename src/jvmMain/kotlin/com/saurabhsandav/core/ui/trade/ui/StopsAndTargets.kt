package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.DefaultTableHeader
import com.saurabhsandav.core.ui.common.table.DefaultTableRow
import com.saurabhsandav.core.ui.common.table.addColumnText
import com.saurabhsandav.core.ui.common.table.rememberTableSchema
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeStop
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTarget
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

@Composable
internal fun StopsAndTargets(
    stops: ImmutableList<TradeStop>,
    onAddStop: (BigDecimal) -> Unit,
    onDeleteStop: (BigDecimal) -> Unit,
    targets: ImmutableList<TradeTarget>,
    onAddTarget: (BigDecimal) -> Unit,
    onDeleteTarget: (BigDecimal) -> Unit,
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        StopsList(
            modifier = Modifier.weight(1F),
            stops = stops,
            onAddStop = onAddStop,
            onDeleteStop = onDeleteStop,
        )

        TargetsList(
            modifier = Modifier.weight(1F),
            targets = targets,
            onAddTarget = onAddTarget,
            onDeleteTarget = onDeleteTarget,
        )
    }
}

@Composable
private fun StopsList(
    stops: ImmutableList<TradeStop>,
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
            addColumn(span = .5f) { stop ->

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
        }

        Divider()

        // Add Stop Form
        AddValueForm(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            addTypeText = "Stop",
            onAdd = onAddStop,
        )
    }
}

@Composable
private fun TargetsList(
    targets: ImmutableList<TradeTarget>,
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
            addColumn(span = .5f) { target ->

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
        }

        Divider()

        // Add Target Form
        AddValueForm(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            addTypeText = "Target",
            onAdd = onAddTarget,
        )
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
private fun AddValueForm(
    modifier: Modifier,
    addTypeText: String,
    onAdd: (BigDecimal) -> Unit,
) {

    var showAddRow by state { false }

    AnimatedContent(
        targetState = showAddRow,
        modifier = modifier,
    ) { targetShowAddRow ->

        if (targetShowAddRow) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {

                var price by state { "" }
                var priceIsError by state { false }

                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        price = it.trim()
                        priceIsError = price.toBigDecimalOrNull() == null
                    },
                    label = { Text(addTypeText) },
                    isError = priceIsError,
                    supportingText = {
                        if (priceIsError) Text("Not a valid price")
                    },
                    singleLine = true,
                )

                TextButton(
                    onClick = {
                        showAddRow = false
                        onAdd(price.toBigDecimal())
                    }
                ) {
                    Text("Add")
                }

                TextButton(
                    onClick = {
                        showAddRow = false
                        price = ""
                    }
                ) {
                    Text("Close")
                }
            }
        } else {

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showAddRow = true },
                shape = RectangleShape,
            ) {
                Text("Add $addTypeText")
            }
        }
    }
}

package com.saurabhsandav.core.ui.trades.detail.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.trades.detail.model.TradeDetailState.TradeStop
import com.saurabhsandav.core.ui.trades.detail.model.TradeDetailState.TradeTarget
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        // Header
        Row(
            modifier = Modifier.height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text(
                text = "Stop",
                modifier = Modifier.weight(1F),
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Risk",
                modifier = Modifier.weight(1F),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1F))
        }

        Divider()

        // Stops list
        stops.forEach { stop ->

            key(stop) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Text(
                        text = stop.priceText,
                        modifier = Modifier.weight(1F),
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = stop.risk,
                        modifier = Modifier.weight(1F),
                        textAlign = TextAlign.Center,
                    )

                    DeleteIconButton(
                        deleteTypeText = "Stop @ ${stop.priceText}",
                        onDelete = { onDeleteStop(stop.price) },
                        modifier = Modifier.weight(1F),
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        // Header
        Row(
            modifier = Modifier.height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text(
                text = "Target",
                modifier = Modifier.weight(1F),
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Profit",
                modifier = Modifier.weight(1F),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1F))
        }

        Divider()

        // Targets list
        targets.forEach { target ->

            key(target) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Text(
                        text = target.priceText,
                        modifier = Modifier.weight(1F),
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = target.profit,
                        modifier = Modifier.weight(1F),
                        textAlign = TextAlign.Center,
                    )

                    DeleteIconButton(
                        deleteTypeText = "Target @ ${target.priceText}",
                        onDelete = { onDeleteTarget(target.price) },
                        modifier = Modifier.weight(1F),
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
    modifier: Modifier,
) {

    var showDeleteConfirmationDialog by state { false }

    IconButton(
        onClick = { showDeleteConfirmationDialog = true },
        modifier = modifier,
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

            Button(
                onClick = { showAddRow = true },
            ) {
                Text("Add $addTypeText")
            }
        }
    }
}

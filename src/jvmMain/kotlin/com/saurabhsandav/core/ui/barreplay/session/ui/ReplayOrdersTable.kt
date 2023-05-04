package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.material.AlertDialog
import androidx.compose.material.SnackbarDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.ReplayOrderListItem
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.*
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ReplayOrdersTable(
    replayOrderItems: ImmutableList<ReplayOrderListItem>,
    onCancelOrder: (Long) -> Unit,
) {

    val schema = rememberTableSchema<ReplayOrderListItem> {
        addColumnText("Execution") { it.execution }
        addColumnText("Broker") { it.broker }
        addColumnText("Ticker") { it.ticker }
        addColumnText("Quantity") { it.quantity }
        addColumn("Type") {
            Text(it.type, color = if (it.type == "BUY") AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Price") { it.price }
        addColumnText("Time") { it.timestamp }
    }

    LazyTable(
        schema = schema,
    ) {

        rows(
            items = replayOrderItems,
            key = { it.id },
        ) { item ->

            var showCancelConfirmationDialog by state { false }

            ContextMenuArea(
                items = {

                    buildList {
                        addAll(
                            listOf(
                                ContextMenuItem("Cancel") { showCancelConfirmationDialog = true },
                            )
                        )
                    }
                },
            ) {

                Column {

                    DefaultTableRow(item, schema)

                    Divider()
                }

                if (showCancelConfirmationDialog) {

                    CancelConfirmationDialog(
                        onDismiss = { showCancelConfirmationDialog = false },
                        onConfirm = { onCancelOrder(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CancelConfirmationDialog(
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
            Text("Are you sure you want to cancel the order?")
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        },
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.colorScheme.surface,
        contentColor = contentColorFor(SnackbarDefaults.backgroundColor),
    )
}

package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.ReplayOrderListItem
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.*

@Composable
internal fun ReplayOrdersTable(
    replayOrderItems: List<ReplayOrderListItem>,
    onCancelOrder: (Long) -> Unit,
) {

    val schema = rememberTableSchema<ReplayOrderListItem> {
        addColumnText("Execution Type") { it.executionType }
        addColumnText("Broker") { it.broker }
        addColumnText("Ticker") { it.ticker }
        addColumnText("Quantity") { it.quantity }
        addColumn("Side") {
            Text(it.side, color = if (it.side == "BUY") AppColor.ProfitGreen else AppColor.LossRed)
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

                    HorizontalDivider()
                }

                if (showCancelConfirmationDialog) {

                    ConfirmationDialog(
                        text = "Are you sure you want to cancel the order?",
                        onDismiss = { showCancelConfirmationDialog = false },
                        onConfirm = { onCancelOrder(item.id) },
                    )
                }
            }
        }
    }
}

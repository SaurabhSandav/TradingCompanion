package ui.opentrades.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.common.AppColor
import ui.common.table.*
import ui.opentrades.model.OpenTradeListEntry

@Composable
internal fun OpenTradesTable(
    openTrades: List<OpenTradeListEntry>,
    onEditTrade: (id: Long) -> Unit,
    onOpenPNLCalculator: (id: Long) -> Unit,
    onDeleteTrade: (id: Long) -> Unit,
    onAddTrade: () -> Unit,
    onCloseTrade: (id: Long) -> Unit,
) {

    val schema = rememberTableSchema<OpenTradeListEntry> {
        addColumnText("Broker", span = 1.5F) { it.broker }
        addColumnText("Ticker", span = 1.5F) { it.ticker }
        addColumnText("Quantity") { it.quantity }
        addColumn("Side") {
            Text(it.side, color = if (it.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Entry") { it.entry }
        addColumnText("Stop") { it.stop }
        addColumnText("Entry Time") { it.entryTime }
        addColumnText("Target") { it.target }
        addColumn(span = .5F) {
            OutlinedButton(
                onClick = { onCloseTrade(it.id) },
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Close")
            }
        }
    }

    LazyTable(
        schema = schema,
    ) {

        rows(
            items = openTrades,
            key = { it.id },
        ) { openTrade ->

            ContextMenuArea(
                items = {
                    listOf(
                        ContextMenuItem("Edit") { onEditTrade(openTrade.id) },
                        ContextMenuItem("PNL Calculator") { onOpenPNLCalculator(openTrade.id) },
                        ContextMenuItem("Delete") { onDeleteTrade(openTrade.id) },
                    )
                },
            ) {

                DefaultTableRow(
                    item = openTrade,
                    schema = schema,
                )
            }
        }

        row {

            Row(
                modifier = Modifier.padding(16.dp).fillParentMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Button(onClick = onAddTrade) {
                    Text("New Trade")
                }
            }
        }
    }
}

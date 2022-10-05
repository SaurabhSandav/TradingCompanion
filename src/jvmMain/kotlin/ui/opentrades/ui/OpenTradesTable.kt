package ui.opentrades.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.common.table.*
import ui.opentrades.model.OpenTradeListEntry

@Composable
internal fun OpenTradesTable(
    openTrades: List<OpenTradeListEntry>,
    onEditTrade: (id: Int) -> Unit,
    onDeleteTrade: (id: Int) -> Unit,
    onAddTrade: () -> Unit,
    onCloseTrade: (id: Int) -> Unit,
) {

    val schema = rememberTableSchema<OpenTradeListEntry> {
        addColumnText("Broker") { it.broker }
        addColumnText("Ticker") { it.ticker }
        addColumnText("Instrument") { it.instrument }
        addColumnText("Quantity") { it.quantity }
        addColumnText("Side") { it.side }
        addColumnText("Entry") { it.entry }
        addColumnText("Stop") { it.stop }
        addColumnText("Entry Time") { it.entryTime }
        addColumnText("Target") { it.target }
        addColumn {
            Button(onClick = { onCloseTrade(it.id) }) {
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

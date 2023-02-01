package ui.tradeorders.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.common.AppColor
import ui.common.table.*
import ui.tradeorders.model.TradeOrderListItem

@Composable
internal fun TradeOrdersTable(
    tradeOrderItems: Map<TradeOrderListItem.DayHeader, List<TradeOrderListItem.Entry>>,
    onDeleteOrder: (id: Long) -> Unit,
) {

    val schema = rememberTableSchema<TradeOrderListItem.Entry> {
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

        tradeOrderItems.forEach { (dayHeader, entries) ->

            stickyHeader {

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {

                    Box(
                        modifier = Modifier.fillParentMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(dayHeader.header)
                    }
                }

                Divider()
            }

            rows(
                items = entries,
                key = { it.id },
            ) { item ->

                ContextMenuArea(
                    items = {
                        listOf(
                            ContextMenuItem("Delete") { onDeleteOrder(item.id) },
                        )
                    },
                ) {

                    Column {

                        DefaultTableRow(item, schema)

                        Divider()
                    }
                }
            }
        }
    }
}

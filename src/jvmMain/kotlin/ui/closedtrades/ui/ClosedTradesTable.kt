package ui.closedtrades.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.closedtrades.model.ClosedTradeListItem
import ui.common.AppColor
import ui.common.table.*

@Composable
internal fun ClosedTradesTable(
    closedTradesItems: Map<ClosedTradeListItem.DayHeader, List<ClosedTradeListItem.Entry>>,
    onEditTrade: (id: Int) -> Unit,
    onDeleteTrade: (id: Int) -> Unit,
) {

    val schema = rememberTableSchema<ClosedTradeListItem.Entry> {
        addColumnText("Broker") { it.broker }
        addColumnText("Ticker") { it.ticker }
        addColumnText("Quantity") { it.quantity }
        addColumn("Side") {
            Text(it.side, color = if (it.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Entry") { it.entry }
        addColumnText("Stop") { it.stop }
        addColumnText("Duration") { it.duration }
        addColumnText("Target") { it.target }
        addColumnText("Exit") { it.exit }
        addColumnText("Maximum Favorable Excursion") { it.maxFavorableExcursion }
        addColumnText("Maximum Adverse Excursion") { it.maxAdverseExcursion }
        addColumn("PNL") {
            Text(it.pnl, color = if (it.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumn("Net PNL") {
            Text(it.netPnl, color = if (it.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Fees") { it.fees }
    }

    LazyTable(
        schema = schema,
    ) {

        closedTradesItems.forEach { (dayHeader, entries) ->

            stickyHeader {

                Surface {
                    Box(
                        modifier = Modifier.fillParentMaxWidth()
                            .background(Color.LightGray)
                            .padding(8.dp),
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
                            ContextMenuItem("Edit") { onEditTrade(item.id) },
                            ContextMenuItem("Delete") { onDeleteTrade(item.id) },
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

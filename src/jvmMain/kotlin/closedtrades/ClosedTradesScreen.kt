package closedtrades

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import table.*

@Composable
internal fun ClosedTradesScreen(
    presenter: ClosedTradesPresenter,
) {

    val state by presenter.state.collectAsState()

    val schema = rememberTableSchema<ClosedTradeListItem.Entry> {
        addColumnText("Broker") { it.broker }
        addColumnText("Ticker") { it.ticker }
        addColumnText("Quantity") { it.quantity }
        addColumn("Side") {
            Text(it.side, color = if (it.side.lowercase() == "long") Color.Green else Color.Red)
        }
        addColumnText("Entry") { it.entry }
        addColumnText("Stop") { it.stop }
        addColumnText("Entry Time") { it.entryTime }
        addColumnText("Target") { it.target }
        addColumnText("Exit") { it.exit }
        addColumnText("Exit Time") { it.exitTime }
        addColumnText("Maximum Favorable Excursion") { it.maxFavorableExcursion }
        addColumnText("Maximum Adverse Excursion") { it.maxAdverseExcursion }
        addColumnText("P&L") { it.pnl }
        addColumnText("Net P&L") { it.netPnl }
        addColumnText("Fees") { it.fees }
        addColumnText("Duration") { it.duration }
    }

    Table(
        schema = schema,
    ) {

        state.closedTradesItems.forEach { (dayHeader, entries) ->

            stickyHeader {
                Box {
                    Text(dayHeader.header)
                }
            }

            rows(
                items = entries,
                key = { it.id },
            ) { item ->

                Column {

                    DefaultTableRow(item, schema)

                    Divider()
                }
            }
        }
    }
}

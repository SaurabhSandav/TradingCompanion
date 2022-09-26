package closedtrades

import Table
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun ClosedTradesScreen(
    presenter: ClosedTradesPresenter,
) {

    val state by presenter.state.collectAsState()

    Table(
        items = state.closedTradesItems,
        key = { it.id },
    ) {

        column(
            header = { Text("Broker") },
            content = { Text(it.broker) },
        )

        column(
            header = { Text("Ticker") },
            content = { Text(it.ticker) },
        )

        column(
            header = { Text("Instrument") },
            content = { Text(it.instrument) },
        )

        column(
            header = { Text("Quantity") },
            content = { Text(it.quantity) },
        )

        column(
            header = { Text("Side") },
            content = { Text(it.side) },
        )

        column(
            header = { Text("Entry") },
            content = { Text(it.entry) },
        )

        column(
            header = { Text("Stop") },
            content = { Text(it.stop) },
        )

        column(
            header = { Text("Entry Time") },
            content = { Text(it.entryTime) },
        )

        column(
            header = { Text("Target") },
            content = { Text(it.target) },
        )

        column(
            header = { Text("Exit") },
            content = { Text(it.exit) },
        )

        column(
            header = { Text("Exit Time") },
            content = { Text(it.exitTime) },
        )

        column(
            header = { Text("Maximum Favorable Excursion") },
            content = { Text(it.maxFavorableExcursion) },
        )

        column(
            header = { Text("Maximum Adverse Excursion") },
            content = { Text(it.maxAdverseExcursion) },
        )

        column(
            header = { Text("P&L") },
            content = { Text(it.pnl) },
        )

        column(
            header = { Text("Net P&L") },
            content = { Text(it.netPnl) },
        )

        column(
            header = { Text("Fees") },
            content = { Text(it.fees) },
        )

        column(
            header = { Text("Duration") },
            content = { Text(it.duration) },
        )
    }
}

@Composable
private fun ClosedTradeListHeader() {

    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = "Broker",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Ticker",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Instrument",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Quantity",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Side",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Entry",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Stop",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Entry Time",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Target",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Exit",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Exit Time",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Maximum Favorable Excursion",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Maximum Adverse Excursion",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "P&L",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Net P&L",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Fees",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Duration",
            modifier = Modifier.weight(1F),
        )
    }
}

@Composable
private fun DayHeader(dayStr: String) {

    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {

        Text(dayStr)
    }
}

@Composable
private fun ClosedTradeListItem(
    entry: ClosedTradeListItem.Entry,
) {

    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = entry.broker,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.ticker,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.instrument,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.quantity,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.side,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.entry,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.stop,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.entryTime,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.target,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.exit,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.exitTime,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.maxFavorableExcursion,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.maxAdverseExcursion,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.pnl,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.netPnl,
            modifier = Modifier.weight(1F).background(
                when {
                    entry.isProfitable -> Color.Green
                    else -> Color.Red
                }
            ),
        )

        Text(
            text = entry.fees,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.duration,
            modifier = Modifier.weight(1F),
        )
    }
}

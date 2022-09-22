package closedtrades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ClosedTradesScreen(
    presenter: ClosedTradesPresenter,
) {

    val state by presenter.state.collectAsState()

    LazyColumn {

        stickyHeader {

            ClosedTradeListHeader()

            Divider()
        }

        items(state.closedTradesItems) { closedTradeItem ->

            when (closedTradeItem) {
                is ClosedTradeListItem.DayHeader -> DayHeader(closedTradeItem.header)
                is ClosedTradeListItem.Entry -> ClosedTradeListItem(
                    entry = closedTradeItem,
                )
            }

            Divider()
        }
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
            text = entry.stop ?: "NA",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.entryTime,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.target ?: "NA",
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
            modifier = Modifier.weight(1F),
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

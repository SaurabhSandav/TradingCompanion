package closedtrades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    Column {

        ClosedTradeListHeader()

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {

            for (closedTradeDetailed in state.closedTradesDetailed) {

                ClosedTradeListItem(
                    closedTradeDetailed = closedTradeDetailed,
                )
            }
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
            text = "R",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Duration",
            modifier = Modifier.weight(1F),
        )
    }

    Divider()
}

@Composable
private fun ClosedTradeListItem(
    closedTradeDetailed: ClosedTradeDetailed,
) {

    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = closedTradeDetailed.broker,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.ticker,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.instrument,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.quantity,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.side,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.entry,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.stop ?: "NA",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.entryTime,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.target ?: "NA",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.exit,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.exitTime,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.maxFavorableExcursion,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.maxAdverseExcursion,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.pnl,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.netPnl,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.fees,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.rValue,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = closedTradeDetailed.duration,
            modifier = Modifier.weight(1F),
        )
    }

    Divider()
}

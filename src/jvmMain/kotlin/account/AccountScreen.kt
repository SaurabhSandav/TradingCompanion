package account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AccountScreen(
    presenter: AccountPresenter,
) {

    val state by presenter.state.collectAsState()

    Column {

        LedgerHeader()

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {

            for (transaction in state.transactions) {

                LedgerItem(
                    transaction = transaction,
                )
            }

            SizingTradeCreator(
                onAddTrade = { },
            )
        }
    }
}

@Composable
private fun LedgerHeader() {

    Row(Modifier.padding(16.dp)) {

        Text(
            text = "Date",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Type",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Amount",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Note",
            modifier = Modifier.weight(1F),
        )
    }

    Divider()
}

@Composable
private fun LedgerItem(
    transaction: Transaction,
) {

    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = transaction.date,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = transaction.type,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = transaction.amount,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = transaction.note,
            modifier = Modifier.weight(1F),
        )
    }
}

@Composable
private fun SizingTradeCreator(
    onAddTrade: (ticker: String) -> Unit,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {

        var ticker by remember { mutableStateOf("") }

        OutlinedTextField(
            value = ticker,
            onValueChange = { ticker = it },
        )

        Button(
            onClick = { onAddTrade(ticker) },
            modifier = Modifier.alignByBaseline(),
        ) {

            Text("New Trade")
        }
    }
}

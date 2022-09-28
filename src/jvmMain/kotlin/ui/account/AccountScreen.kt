package ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.common.AppColor
import ui.common.state
import ui.common.table.*

@Composable
internal fun AccountScreen(
    presenter: AccountPresenter,
) {

    val state by presenter.state.collectAsState()

    val schema = rememberTableSchema<Transaction> {
        addColumnText("Date") { it.date }
        addColumnText("Type") { it.type }
        addColumn("Amount") {
            Text(it.amount, color = if (it.type == "Credit") AppColor.ProfitGreen else AppColor.LossRed)
        }
        addColumnText("Note") { it.note }
    }

    LazyTable(
        schema = schema,
    ) {

        rows(
            items = state.transactions,
        )

        row {
            SizingTradeCreator(
                onAddTrade = { },
            )
        }
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

        var ticker by state { "" }

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

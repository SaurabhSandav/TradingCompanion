package com.saurabhsandav.core.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.app.LocalAppWindowState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.*

@Composable
internal fun AccountScreen(
    presenter: AccountPresenter,
) {

    val state by presenter.state.collectAsState()
    val appWindowState = LocalAppWindowState.current

    // Set window title
    LaunchedEffect(appWindowState) { appWindowState.title = "Account" }

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

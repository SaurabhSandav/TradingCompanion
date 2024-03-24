package com.saurabhsandav.core.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun AccountScreen(
    transactions: List<Transaction>,
) {

    // Set window title
    WindowTitle("Account")

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

        rows(items = transactions)

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
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.rowHorizontalSpacing,
            alignment = Alignment.CenterHorizontally,
        ),
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

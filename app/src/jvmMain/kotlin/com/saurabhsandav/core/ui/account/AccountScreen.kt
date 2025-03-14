package com.saurabhsandav.core.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
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
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.content
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun AccountScreen(transactions: List<Transaction>) {

    // Set window title
    WindowTitle("Account")

    LazyTable(
        headerContent = {
            TransactionTableSchema.SimpleHeader {
                date.text { "Date" }
                type.text { "Type" }
                amount.text { "Amount" }
                note.text { "Note" }
            }
        },
    ) {

        items(items = transactions) { item ->

            TransactionTableSchema.SimpleRow(Modifier.animateItem()) {

                date.text { item.date }
                type.text { item.type }
                amount.content {
                    Text(item.amount, color = if (item.type == "Credit") AppColor.ProfitGreen else AppColor.LossRed)
                }
                note.text { item.note }
            }
        }

        item {
            SizingTradeCreator(
                onAddTrade = { },
            )
        }
    }
}

@Composable
private fun SizingTradeCreator(onAddTrade: (ticker: String) -> Unit) {

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

private object TransactionTableSchema : TableSchema() {

    val date = cell()
    val type = cell()
    val amount = cell()
    val note = cell()
}

package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.trade.model.TradeState.Details

@Composable
internal fun Details(details: Details) {

    Column(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        val schema = rememberTableSchema<Details> {
            addColumnText("Broker", span = 1.5F) { it.broker }
            addColumnText("Ticker") { it.ticker }
            addColumn("Side") {

                Text(
                    text = it.side,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (it.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed,
                )
            }
            addColumnText("Quantity") { it.quantity }
            addColumnText("Avg. Entry") { it.entry }
            addColumnText("Avg. Exit") { it.exit ?: "NA" }
            addColumn("Duration") {

                Text(
                    text = it.duration.collectAsState("").value,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            addColumn("PNL") {

                Text(
                    text = it.pnl,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (it.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                )
            }
            addColumn("Net PNL") {

                Text(
                    text = it.netPnl,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (it.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                )
            }
            addColumnText("Fees") { it.fees }
        }

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            DefaultTableHeader(schema = schema)

            Divider()

            DefaultTableRow(
                item = details,
                schema = schema,
            )
        }
    }
}

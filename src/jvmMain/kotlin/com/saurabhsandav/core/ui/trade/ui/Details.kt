package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.trade.model.TradeState.Details

@Composable
internal fun Details(details: Details) {

    Column(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Broker", Modifier.weight(2F))

            Text("Ticker", Modifier.weight(1F))

            Text("Side", Modifier.weight(1F))

            Text("Quantity", Modifier.weight(1F))

            Text("Avg. Entry", Modifier.weight(1F))

            Text("Avg. Exit", Modifier.weight(1F))

            Text("Duration", Modifier.weight(2F))

            Text("PNL", Modifier.weight(1F))

            Text("Net PNL", Modifier.weight(1F))

            Text("Fees", Modifier.weight(1F))
        }

        Divider()

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text(details.broker, Modifier.weight(2F))

            Text(details.ticker, Modifier.weight(1F))

            Text(
                text = details.side,
                modifier = Modifier.weight(1F),
                color = if (details.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed,
            )

            Text(details.quantity, Modifier.weight(1F))

            Text(details.entry, Modifier.weight(1F))

            Text(details.exit ?: "NA", Modifier.weight(1F))

            Text(details.duration, Modifier.weight(2F))

            Text(
                text = details.pnl,
                modifier = Modifier.weight(1F),
                color = if (details.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
            )

            Text(
                text = details.netPnl,
                modifier = Modifier.weight(1F),
                color = if (details.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed,
            )

            Text(details.fees, Modifier.weight(1F))
        }
    }
}

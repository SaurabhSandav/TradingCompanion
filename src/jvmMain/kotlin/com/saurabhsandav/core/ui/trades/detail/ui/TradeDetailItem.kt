package com.saurabhsandav.core.ui.trades.detail.ui

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
import com.saurabhsandav.core.ui.trades.detail.model.TradeDetailState.TradeDetail

@Composable
internal fun TradeDetailItem(detail: TradeDetail) {

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

            Text(detail.broker, Modifier.weight(2F))

            Text(detail.ticker, Modifier.weight(1F))

            Text(
                text = detail.side,
                modifier = Modifier.weight(1F),
                color = if (detail.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed,
            )

            Text(detail.quantity, Modifier.weight(1F))

            Text(detail.entry, Modifier.weight(1F))

            Text(detail.exit ?: "NA", Modifier.weight(1F))

            Text(detail.duration, Modifier.weight(2F))

            Text(
                text = detail.pnl,
                modifier = Modifier.weight(1F),
                color = if (detail.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
            )

            Text(
                text = detail.netPnl,
                modifier = Modifier.weight(1F),
                color = if (detail.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed,
            )

            Text(detail.fees, Modifier.weight(1F))
        }
    }
}

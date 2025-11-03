package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.AnimatedVisibilityForNullable
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.trade.model.TradeState.Details
import com.saurabhsandav.core.ui.trade.model.TradeState.Details.Duration.Closed
import com.saurabhsandav.core.ui.trade.model.TradeState.Details.Duration.Open

@Composable
internal fun Details(
    details: Details,
    onOpenChart: () -> Unit,
    modifier: Modifier = Modifier,
) {

    TradeTileContainer(modifier) {

        TradeTile(
            title = "Broker",
            value = details.broker,
        )

        TradeTile(
            title = "Symbol",
            value = details.ticker,
        )

        TradeTile(
            title = "Side",
            value = {

                Text(
                    text = details.side,
                    color = if (details.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed,
                )
            },
        )

        TradeTile(
            title = "Quantity",
            value = details.quantity,
        )

        if (details.lots != null) {

            TradeTile(
                title = "Lots",
                value = details.lots,
            )
        }

        TradeTile(
            title = "Avg. Entry",
            value = details.entry,
        )

        AnimatedVisibilityForNullable(details.exit) { exit ->

            TradeTile(
                title = "Avg. Exit",
                value = exit,
            )
        }

        TradeTile(
            title = "Duration",
            value = when (val duration = details.duration) {
                is Open -> duration.flow.collectAsState("").value
                is Closed -> duration.str
            },
        )

        AnimatedVisibilityForNullable(details.pnl) { pnl ->

            TradeTile(
                title = "PNL",
                value = {

                    Text(
                        text = pnl,
                        color = if (details.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                    )
                },
            )
        }

        AnimatedVisibilityForNullable(details.netPnl) { netPnl ->

            TradeTile(
                title = "Net PNL",
                value = {

                    Text(
                        text = netPnl,
                        color = if (details.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                    )
                },
            )
        }

        AnimatedVisibilityForNullable(details.fees) { fees ->

            TradeTile(
                title = "Fees",
                value = fees,
            )
        }

        Card(
            modifier = Modifier.fillMaxRowHeight(),
        ) {

            TextButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(onClick = onOpenChart),
                onClick = onOpenChart,
                shape = CardDefaults.shape,
            ) {

                Text("CHART")

                Spacer(Modifier.width(ButtonDefaults.IconSpacing))

                Icon(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open Chart",
                )
            }
        }
    }
}

@Composable
private fun TradeTile(
    title: String,
    value: String,
) {

    TradeTile(
        title = title,
        value = { Text(value) },
    )
}

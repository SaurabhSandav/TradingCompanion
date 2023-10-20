package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.trade.model.TradeState.MfeAndMae

@Composable
internal fun MfeAndMae(mfeAndMae: MfeAndMae) {

    Column(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            // Header
            Row(
                modifier = Modifier.height(64.dp).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    modifier = Modifier.weight(1F),
                    text = "Maximum Adverse Excursion",
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = "Maximum Favorable Excursion",
                )
            }

            Divider()

            Row(
                modifier = Modifier.height(48.dp).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    modifier = Modifier.weight(1F),
                    text = "Price",
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = "PNL",
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = "Price",
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = "PNL",
                )
            }

            Divider()

            Row(
                modifier = Modifier
                    .combinedClickable { }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    modifier = Modifier.weight(1F),
                    text = mfeAndMae.maePrice,
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = mfeAndMae.maePnl,
                    color = AppColor.LossRed,
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = mfeAndMae.mfePrice,
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = mfeAndMae.mfePnl,
                    color = AppColor.ProfitGreen,
                )
            }
        }
    }
}

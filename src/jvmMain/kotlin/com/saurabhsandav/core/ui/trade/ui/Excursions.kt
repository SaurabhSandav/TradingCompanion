package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
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
import com.saurabhsandav.core.ui.trade.model.TradeState.Excursions

@Composable
internal fun Excursions(excursions: Excursions) {

    Column(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            // Header
            Row(
                modifier = Modifier.height(48.dp).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    modifier = Modifier.weight(1F),
                    text = "Trade MAE",
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = "Session MAE",
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = "Trade MFE",
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = "Session MFE",
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .combinedClickable { }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    modifier = Modifier.weight(1F),
                    text = excursions.maeInTrade,
                    color = AppColor.LossRed,
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = excursions.maeInSession,
                    color = AppColor.LossRed,
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = excursions.mfeInTrade,
                    color = AppColor.ProfitGreen,
                )

                Text(
                    modifier = Modifier.weight(1F),
                    text = excursions.mfeInSession,
                    color = AppColor.ProfitGreen,
                )
            }
        }
    }
}

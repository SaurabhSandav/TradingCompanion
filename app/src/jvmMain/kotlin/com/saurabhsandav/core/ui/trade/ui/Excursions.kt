package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.trade.model.TradeState.Excursions

@Composable
internal fun Excursions(
    excursions: Excursions,
    modifier: Modifier = Modifier,
) {

    TradeTileContainer(modifier) {

        TradeTile("Trade MAE") {

            Text(
                text = excursions.maeInTrade,
                color = AppColor.LossRed,
            )
        }

        TradeTile("Trade MFE") {

            Text(
                text = excursions.mfeInTrade,
                color = AppColor.ProfitGreen,
            )
        }

        TradeTile("Session MAE") {

            Text(
                text = excursions.maeInSession,
                color = AppColor.LossRed,
            )
        }

        TradeTile("Session MFE") {

            Text(
                text = excursions.mfeInSession,
                color = AppColor.ProfitGreen,
            )
        }
    }
}


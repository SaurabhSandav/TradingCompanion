package com.saurabhsandav.core.ui.charts.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.stockchart.StockChartsState

@Immutable
internal data class ChartsState(
    val chartsState: StockChartsState?,
    val showCandleDataLoginConfirmation: Boolean,
    val errors: List<UIErrorMessage>,
    val eventSink: (ChartsEvent) -> Unit,
) {

    @Immutable
    data class ProfileTradeId(
        val profileId: ProfileId,
        val tradeId: TradeId,
    )
}

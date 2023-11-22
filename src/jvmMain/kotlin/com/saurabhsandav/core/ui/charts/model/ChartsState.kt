package com.saurabhsandav.core.ui.charts.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ChartsState(
    val chartsState: StockChartsState?,
    val showCandleDataLoginConfirmation: Boolean,
    val markedTrades: ImmutableList<ProfileTradeId>,
    val errors: List<UIErrorMessage>,
    val eventSink: (ChartsEvent) -> Unit,
)

package com.saurabhsandav.core.ui.charts.model

import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.stockchart.StockChartsState

internal data class ChartsState(
    val chartsState: StockChartsState?,
    val showCandleDataLoginConfirmation: Boolean,
    val errors: List<UIErrorMessage>,
    val eventSink: (ChartsEvent) -> Unit,
)

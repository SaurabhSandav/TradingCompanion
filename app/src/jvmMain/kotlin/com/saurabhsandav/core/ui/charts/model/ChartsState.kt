package com.saurabhsandav.core.ui.charts.model

import com.saurabhsandav.core.ui.stockchart.StockChartsState

internal data class ChartsState(
    val chartsState: StockChartsState?,
    val showCandleDataLoginConfirmation: Boolean,
    val eventSink: (ChartsEvent) -> Unit,
)

package com.saurabhsandav.core.ui.charts.model

import com.saurabhsandav.core.ui.stockchart.StockChartsState

internal data class ChartsState(
    val chartsState: StockChartsState?,
    val eventSink: (ChartsEvent) -> Unit,
)

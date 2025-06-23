package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.trading.core.Timeframe

data class StockChartParams(
    val ticker: String,
    val timeframe: Timeframe,
)

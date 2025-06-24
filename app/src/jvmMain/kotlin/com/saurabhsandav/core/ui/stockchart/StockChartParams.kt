package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.trading.core.Timeframe

data class StockChartParams(
    val ticker: String,
    val timeframe: Timeframe,
)

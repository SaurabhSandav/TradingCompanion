package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.Stable
import com.saurabhsandav.core.trading.Timeframe

@Stable
data class StockChartParams(
    val ticker: String,
    val timeframe: Timeframe,
)

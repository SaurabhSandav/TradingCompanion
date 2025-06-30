package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.core.Timeframe

data class StockChartParams(
    val symbolId: SymbolId,
    val timeframe: Timeframe,
)

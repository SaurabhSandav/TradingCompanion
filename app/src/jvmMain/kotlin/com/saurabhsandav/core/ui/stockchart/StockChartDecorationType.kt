package com.saurabhsandav.core.ui.stockchart

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

sealed class StockChartDecorationType {

    class Charts(
        val onOpenTradeReview: () -> Unit,
    ) : StockChartDecorationType()

    class BarReplay(
        val customControls: @Composable ColumnScope.(StockChart) -> Unit,
    ) : StockChartDecorationType()
}

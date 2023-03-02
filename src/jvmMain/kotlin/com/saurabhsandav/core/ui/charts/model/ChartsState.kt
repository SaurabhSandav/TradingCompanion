package com.saurabhsandav.core.ui.charts.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState

@Immutable
data class ChartsState(
    val tabsState: StockChartTabsState,
    val chartPageState: ChartPageState,
    val chartInfo: ChartInfo,
    val fyersLoginWindowState: FyersLoginWindow,
    val errors: List<UIErrorMessage>,
) {

    @Immutable
    data class ChartInfo(
        val ticker: String,
        val timeframe: Timeframe,
    )

    @Immutable
    sealed class FyersLoginWindow {

        @Immutable
        internal class Open(val fyersLoginState: FyersLoginState) : FyersLoginWindow()

        @Immutable
        object Closed : FyersLoginWindow()
    }
}

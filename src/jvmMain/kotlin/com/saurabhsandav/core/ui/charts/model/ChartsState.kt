package com.saurabhsandav.core.ui.charts.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState

@Immutable
internal data class ChartsState(
    val tabsState: StockChartTabsState,
    val chartPageState: ChartPageState,
    val chartInfo: ChartInfo,
    val fyersLoginWindowState: FyersLoginWindow,
    val errors: List<UIErrorMessage>,
) {

    @Immutable
    internal data class ChartInfo(
        val stockChart: StockChart? = null,
    )

    @Immutable
    sealed class FyersLoginWindow {

        @Immutable
        internal class Open(val fyersLoginState: FyersLoginState) : FyersLoginWindow()

        @Immutable
        object Closed : FyersLoginWindow()
    }
}

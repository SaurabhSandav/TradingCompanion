package ui.charts.model

import androidx.compose.runtime.Immutable
import trading.Timeframe
import ui.common.UIErrorMessage
import ui.common.chart.state.ChartPageState
import ui.fyerslogin.FyersLoginState
import ui.stockchart.StockChartTabsState

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
        val symbol: String,
        val timeframe: Timeframe,
        val legendValues: LegendValues = LegendValues(),
    )

    @Immutable
    data class LegendValues(
        val open: String = "",
        val high: String = "",
        val low: String = "",
        val close: String = "",
        val volume: String = "",
        val ema9: String = "",
        val vwap: String = "",
    )

    @Immutable
    sealed class FyersLoginWindow {

        @Immutable
        internal class Open(val fyersLoginState: FyersLoginState) : FyersLoginWindow()

        @Immutable
        object Closed : FyersLoginWindow()
    }
}

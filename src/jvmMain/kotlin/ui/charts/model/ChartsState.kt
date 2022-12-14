package ui.charts.model

import androidx.compose.runtime.Immutable
import ui.common.UIErrorMessage
import ui.common.chart.state.ChartState
import ui.fyerslogin.FyersLoginState

@Immutable
data class ChartsState(
    val tabsState: TabsState,
    val chartState: ChartState,
    val chartInfo: ChartInfo,
    val fyersLoginWindowState: FyersLoginWindow,
    val errors: List<UIErrorMessage>,
) {

    @Immutable
    data class TabsState(
        val tabs: List<TabInfo>,
        val selectedTabIndex: Int,
    ) {

        @Immutable
        data class TabInfo(
            val id: Int,
            val title: String,
        )
    }

    @Immutable
    data class ChartInfo(
        val symbol: String,
        val timeframe: String,
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

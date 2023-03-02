package com.saurabhsandav.core.ui.charts.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.charts.model.ChartsState
import com.saurabhsandav.core.ui.charts.model.ChartsState.FyersLoginWindow
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginWindow
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState
import com.saurabhsandav.core.ui.stockchart.StockCharts

@Composable
internal fun ChartsScreen(
    tabsState: StockChartTabsState,
    chartPageState: ChartPageState,
    chartInfo: ChartsState.ChartInfo,
    onTickerChange: (String) -> Unit,
    onTimeframeChange: (Timeframe) -> Unit,
    fyersLoginWindowState: FyersLoginWindow,
    errors: List<UIErrorMessage>,
) {

    Column {

        StockCharts(
            modifier = Modifier.weight(1F),
            pageState = chartPageState,
            stockChart = chartInfo.stockChart,
            onTickerChange = onTickerChange,
            onTimeframeChange = onTimeframeChange,
            tabsState = tabsState,
        )

        val snackbarHostState = remember { SnackbarHostState() }

        // Errors
        errors.forEach { errorMessage ->

            ErrorSnackbar(snackbarHostState, errorMessage)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.animateContentSize().align(Alignment.CenterHorizontally),
        )
    }

    // Fyers login window
    if (fyersLoginWindowState is FyersLoginWindow.Open) {

        FyersLoginWindow(fyersLoginWindowState.fyersLoginState)
    }
}

package ui.charts.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import trading.Timeframe
import ui.charts.model.ChartsState
import ui.charts.model.ChartsState.FyersLoginWindow
import ui.common.ErrorSnackbar
import ui.common.UIErrorMessage
import ui.common.chart.state.ChartPageState
import ui.common.controls.ListSelectionField
import ui.common.timeframeFromLabel
import ui.common.toLabel
import ui.fyerslogin.FyersLoginWindow
import ui.stockchart.StockChartTabsState
import ui.stockchart.StockCharts
import ui.stockchart.ui.StockChartsTabControls
import utils.NIFTY50

@Composable
fun ChartsScreen(
    tabsState: StockChartTabsState,
    chartPageState: ChartPageState,
    chartInfo: ChartsState.ChartInfo,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (Timeframe) -> Unit,
    fyersLoginWindowState: FyersLoginWindow,
    errors: List<UIErrorMessage>,
) {

    Column {

        StockCharts(
            modifier = Modifier.weight(1F),
            pageState = chartPageState,
            tabsState = tabsState,
        ) {

            LegendItem("Open", chartInfo.legendValues.open)

            LegendItem("High", chartInfo.legendValues.high)

            LegendItem("Low", chartInfo.legendValues.low)

            LegendItem("Close", chartInfo.legendValues.close)

            LegendItem("Volume", chartInfo.legendValues.volume)

            LegendItem("EMA (9)", chartInfo.legendValues.ema9)

            LegendItem("VWAP", chartInfo.legendValues.vwap)

            Divider()

            ListSelectionField(
                items = NIFTY50,
                selection = chartInfo.symbol,
                onSelection = onSymbolChange,
                label = { Text("Ticker") },
            )

            ListSelectionField(
                items = remember { Timeframe.values().map { it.toLabel() } },
                selection = chartInfo.timeframe.toLabel(),
                onSelection = { onTimeframeChange(timeframeFromLabel(it)) },
                label = { Text("Timeframe") },
            )

            Divider()

            StockChartsTabControls(tabsState)
        }

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

@Composable
private fun LegendItem(
    title: String,
    value: String,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {

        Text(title)

        Text(value)
    }
}

package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.ReplayChartInfo
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartDecorationType
import com.saurabhsandav.core.ui.stockchart.StockCharts
import com.saurabhsandav.core.ui.stockchart.StockChartsState

@Composable
internal fun ReplayCharts(
    onCloseRequest: () -> Unit,
    chartsState: StockChartsState,
    chartInfo: (StockChart) -> ReplayChartInfo,
    replayFullBar: Boolean,
    onAdvanceReplay: () -> Unit,
    onAdvanceReplayByBar: () -> Unit,
    isAutoNextEnabled: Boolean,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
    isTradingEnabled: Boolean,
    onBuy: (StockChart) -> Unit,
    onSell: (StockChart) -> Unit,
) {

    val isAutoNextEnabledUpdated by rememberUpdatedState(isAutoNextEnabled)

    StockCharts(
        onCloseRequest = onCloseRequest,
        state = chartsState,
        windowTitle = "Bar Replay Charts",
        decorationType = StockChartDecorationType.BarReplay { stockChart ->

            ReplayChartControls(
                stockChart = stockChart,
                chartInfo = chartInfo,
                replayFullBar = replayFullBar,
                onAdvanceReplay = onAdvanceReplay,
                onAdvanceReplayByBar = onAdvanceReplayByBar,
                isAutoNextEnabled = isAutoNextEnabled,
                onIsAutoNextEnabledChange = onIsAutoNextEnabledChange,
                isTradingEnabled = isTradingEnabled,
                onBuy = onBuy,
                onSell = onSell,
            )
        },
        customShortcuts = customShortCuts@{ keyEvent ->

            if (keyEvent.type != KeyEventType.KeyDown) return@customShortCuts false

            when (keyEvent.key) {
                Key.A -> onAdvanceReplay()
                Key.S -> onAdvanceReplayByBar()
                Key.D -> onIsAutoNextEnabledChange(!isAutoNextEnabledUpdated)
                else -> return@customShortCuts false
            }

            return@customShortCuts true
        },
    )
}

@Composable
private fun ReplayChartControls(
    stockChart: StockChart,
    chartInfo: (StockChart) -> ReplayChartInfo,
    replayFullBar: Boolean,
    onAdvanceReplay: () -> Unit,
    onAdvanceReplayByBar: () -> Unit,
    isAutoNextEnabled: Boolean,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
    isTradingEnabled: Boolean,
    onBuy: (StockChart) -> Unit,
    onSell: (StockChart) -> Unit,
) {

    val currentChartInfo = remember(chartInfo) { chartInfo(stockChart) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {

        Text("Time")

        val replayTime by currentChartInfo.replayTime.collectAsState("")

        Text(replayTime, textAlign = TextAlign.End)
    }

    if (!replayFullBar) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {

            Text("Candle State")

            val candleState by currentChartInfo.candleState.collectAsState("")

            Text(candleState, textAlign = TextAlign.End)
        }
    }

    HorizontalDivider()

    ReplayControls(
        replayFullBar = replayFullBar,
        onAdvanceReplay = onAdvanceReplay,
        onAdvanceReplayByBar = onAdvanceReplayByBar,
        isAutoNextEnabled = isAutoNextEnabled,
        onIsAutoNextEnabledChange = onIsAutoNextEnabledChange,
        isTradingEnabled = isTradingEnabled,
        onBuy = { onBuy(stockChart) },
        onSell = { onSell(stockChart) },
    )
}

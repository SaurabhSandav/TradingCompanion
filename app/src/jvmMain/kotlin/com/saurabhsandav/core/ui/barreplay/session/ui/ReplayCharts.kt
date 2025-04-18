package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.ReplayChartInfo
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartDecorationType
import com.saurabhsandav.core.ui.stockchart.StockCharts
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import com.saurabhsandav.core.ui.theme.dimens

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

            val defaultCondition = keyEvent.isAltPressed && keyEvent.type == KeyEventType.KeyDown
            if (!defaultCondition) return@customShortCuts false

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

    Row(
        modifier = Modifier.height(48.dp)
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.containerPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.rowHorizontalSpacing,
            alignment = Alignment.CenterHorizontally,
        ),
    ) {

        val currentChartInfo = remember(chartInfo) { chartInfo(stockChart) }
        val replayTime by currentChartInfo.replayTime.collectAsState("")

        val text = when {
            replayFullBar -> replayTime
            else -> {

                val candleState by currentChartInfo.candleState.collectAsState("")

                "$replayTime ($candleState)"
            }
        }

        Text(text)

        Spacer(Modifier.weight(1F))

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
}

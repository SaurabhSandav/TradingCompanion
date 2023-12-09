package com.saurabhsandav.core.ui.barreplay.session.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.saurabhsandav.core.ui.barreplay.session.model.ReplaySessionState.ReplayChartInfo
import com.saurabhsandav.core.ui.stockchart.StockChart
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

    StockCharts(
        state = chartsState,
        windowTitle = "Bar Replay Charts",
        onCloseRequest = onCloseRequest,
    ) { stockChart ->

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

        Divider()

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

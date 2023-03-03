package com.saurabhsandav.core.ui.barreplay.charts.ui

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
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartInfo
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockCharts
import com.saurabhsandav.core.ui.stockchart.StockChartsState

@Composable
internal fun ReplayCharts(
    onNewReplay: () -> Unit,
    chartsState: StockChartsState,
    chartInfo: (StockChart) -> ReplayChartInfo,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
) {

    StockCharts(
        state = chartsState,
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

        Divider()

        ReplayControls(
            onNewReplay = onNewReplay,
            onReset = onReset,
            onNext = onNext,
            onIsAutoNextEnabledChange = onIsAutoNextEnabledChange,
        )
    }
}

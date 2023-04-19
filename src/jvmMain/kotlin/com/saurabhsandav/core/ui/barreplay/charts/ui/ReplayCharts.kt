package com.saurabhsandav.core.ui.barreplay.charts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartsState.ReplayChartInfo
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.profiles.ProfileSwitcher
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
    selectedProfileId: Long?,
    onSelectProfile: (Long) -> Unit,
    onBuy: (StockChart) -> Unit,
    onSell: (StockChart) -> Unit,
) {

    StockCharts(
        state = chartsState,
        windowTitle = "Bar Replay",
        onCloseRequest = onNewReplay,
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

        Divider()

        ProfileSwitcher(
            modifier = Modifier.fillMaxWidth(),
            selectedProfileId = selectedProfileId,
            onSelectProfile = onSelectProfile,
            trainingOnly = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            Button(
                modifier = Modifier.weight(1F),
                onClick = { onBuy(stockChart) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColor.ProfitGreen),
                content = { Text("BUY") },
                enabled = selectedProfileId != null,
            )

            Button(
                modifier = Modifier.weight(1F),
                onClick = { onSell(stockChart) },
                colors = ButtonDefaults.buttonColors(containerColor = AppColor.LossRed),
                content = { Text("SELL") },
                enabled = selectedProfileId != null,
            )
        }
    }
}

package ui.barreplay.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.barreplay.ReplayChartState
import ui.barreplay.model.BarReplayFormFields
import ui.common.ResizableChart

@Composable
internal fun ReplayChart(
    chartState: ReplayChartState,
    fields: BarReplayFormFields,
    areReplayControlsEnabled: Boolean,
    onNewReplay: () -> Unit,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
) {

    Row(Modifier.fillMaxSize()) {

        ReplayControls(
            fields = fields,
            onNewReplay = onNewReplay,
            onReset = onReset,
            onNext = onNext,
            onSymbolChange = onSymbolChange,
            onTimeframeChange = onTimeframeChange,
            onIsAutoNextEnabledChange = onIsAutoNextEnabledChange,
            enabled = areReplayControlsEnabled,
        )

        ResizableChart(
            chart = chartState.chart,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

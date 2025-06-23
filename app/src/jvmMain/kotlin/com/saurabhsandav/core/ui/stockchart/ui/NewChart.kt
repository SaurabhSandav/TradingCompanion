package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionField
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionType
import com.saurabhsandav.trading.core.Timeframe

@Composable
internal fun NewChartForm(onInitializeChart: (String, Timeframe) -> Unit) {

    Column(
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center).width(IntrinsicSize.Min),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
    ) {

        var ticker by state<String?> { null }
        var timeframe by state<Timeframe?> { null }

        TickerSelectionField(
            type = TickerSelectionType.Regular,
            selected = ticker,
            onSelect = { ticker = it },
        )

        OutlinedListSelectionField(
            items = Timeframe.entries,
            itemText = { it.toLabel() },
            selection = timeframe,
            onSelect = { timeframe = it },
            label = { Text("Timeframe") },
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val ticker = ticker ?: return@Button
                val timeframe = timeframe ?: return@Button
                onInitializeChart(ticker, timeframe)
            },
            enabled = ticker != null && timeframe != null,
        ) {

            Text("Open Chart")
        }
    }
}

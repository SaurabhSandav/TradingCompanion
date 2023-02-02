package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState

@Composable
internal fun StockChartsTabControls(
    state: StockChartTabsState,
) {

    Button(
        onClick = state::moveTabBackward,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Move Tab Backward")
    }

    Button(
        onClick = state::moveTabForward,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Move Tab Forward")
    }
}

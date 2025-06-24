package com.saurabhsandav.core.ui.settings.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.saurabhsandav.core.trading.core.Timeframe
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.toLabel

@Composable
internal fun TradingPreferences(
    defaultTimeframe: Timeframe,
    onDefaultTimeframeChange: (Timeframe) -> Unit,
) {

    DefaultTimeframePreference(
        items = remember { enumValues<Timeframe>().toList() },
        selectedItem = defaultTimeframe,
        onDefaultTimeframeChange = onDefaultTimeframeChange,
    )
}

@Composable
private fun DefaultTimeframePreference(
    items: List<Timeframe>,
    selectedItem: Timeframe,
    onDefaultTimeframeChange: (Timeframe) -> Unit,
) {

    Preference(
        headlineContent = { Text("Timeframe") },
        trailingContent = {

            OutlinedListSelectionField(
                items = items,
                itemText = { it.toLabel() },
                selection = selectedItem,
                onSelect = onDefaultTimeframeChange,
            )
        },
    )
}

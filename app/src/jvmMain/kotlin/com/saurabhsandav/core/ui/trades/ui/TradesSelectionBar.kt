package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.runtime.Composable
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.ui.common.SelectionBar
import com.saurabhsandav.core.ui.common.SelectionManager

@Composable
internal fun TradesSelectionBar(
    selectionManager: SelectionManager<TradeId>,
) {

    SelectionBar(selectionManager) {
    }
}

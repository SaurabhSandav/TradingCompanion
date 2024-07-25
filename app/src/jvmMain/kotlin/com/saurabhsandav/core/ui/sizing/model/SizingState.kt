package com.saurabhsandav.core.ui.sizing.model

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.SizingTradeId
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import java.util.*

internal data class SizingState(
    val sizedTrades: List<SizedTrade>,
    val executionFormWindowsManager: AppWindowsManager<TradeExecutionFormParams>,
    val eventSink: (SizingEvent) -> Unit,
) {

    internal data class SizedTrade(
        val id: SizingTradeId,
        val ticker: String,
        val entry: String,
        val stop: String,
        val side: String,
        val spread: String,
        val calculatedQuantity: String,
        val maxAffordableQuantity: String,
        val target: String,
        val color: Color,
    )

    internal data class TradeExecutionFormParams(
        val id: UUID,
        val profileId: ProfileId,
        val formType: TradeExecutionFormType,
    )
}

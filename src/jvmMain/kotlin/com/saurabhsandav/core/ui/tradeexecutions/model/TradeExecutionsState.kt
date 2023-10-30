package com.saurabhsandav.core.ui.tradeexecutions.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.UIErrorMessage
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TradeExecutionsState(
    val todayExecutions: ImmutableList<TradeExecutionEntry>,
    val pastExecutions: ImmutableList<TradeExecutionEntry>,
    val selectionManager: SelectionManager<TradeExecutionEntry>,
    val errors: ImmutableList<UIErrorMessage>,
    val eventSink: (TradeExecutionsEvent) -> Unit,
) {

    @Immutable
    internal data class TradeExecutionEntry(
        val profileTradeExecutionId: ProfileTradeExecutionId,
        val broker: String,
        val ticker: String,
        val quantity: String,
        val side: String,
        val price: String,
        val timestamp: String,
        val locked: Boolean,
    )

    @Immutable
    data class ProfileTradeExecutionId(
        val profileId: ProfileId,
        val executionId: TradeExecutionId,
    )
}

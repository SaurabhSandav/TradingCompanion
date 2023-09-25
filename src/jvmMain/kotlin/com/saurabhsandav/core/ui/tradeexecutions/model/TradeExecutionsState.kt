package com.saurabhsandav.core.ui.tradeexecutions.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.UIErrorMessage
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TradeExecutionsState(
    val executionsByDays: ImmutableList<TradeExecutionsByDay>,
    val selectionManager: SelectionManager<TradeExecutionEntry>,
    val errors: ImmutableList<UIErrorMessage>,
    val eventSink: (TradeExecutionsEvent) -> Unit,
) {

    @Immutable
    data class TradeExecutionsByDay(
        val dayHeader: String,
        val executions: ImmutableList<TradeExecutionEntry>,
    )

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
        val profileId: Long,
        val executionId: Long,
    )
}

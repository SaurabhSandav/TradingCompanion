package com.saurabhsandav.core.ui.trades.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

@Immutable
internal data class TradesState(
    val tradesByDays: ImmutableList<TradesByDay>,
    val chartWindowsManager: AppWindowsManager<TradeChartWindowParams>,
    val errors: ImmutableList<UIErrorMessage>,
    val eventSink: (TradesEvent) -> Unit,
) {

    @Immutable
    data class TradesByDay(
        val dayHeader: String,
        val trades: ImmutableList<TradeEntry>,
    )

    @Immutable
    internal data class TradeEntry(
        val profileTradeId: ProfileTradeId,
        val broker: String,
        val ticker: String,
        val side: String,
        val quantity: String,
        val entry: String,
        val exit: String?,
        val entryTime: String,
        val duration: Flow<String>,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
        val fees: String,
    )

    @Immutable
    data class ProfileTradeId(
        val profileId: Long,
        val tradeId: Long,
    )
}

package com.saurabhsandav.core.ui.trades.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TradesState(
    val tradesItems: ImmutableList<TradeListItem>,
    val tradeDetailWindowsManager: AppWindowsManager<ProfileTradeId>,
    val chartWindowsManager: AppWindowsManager<TradeChartWindowParams>,
    val fyersLoginWindowState: FyersLoginWindow,
) {

    @Immutable
    internal sealed class FyersLoginWindow {

        @Immutable
        class Open(val fyersLoginState: FyersLoginState) : FyersLoginWindow()

        @Immutable
        data object Closed : FyersLoginWindow()
    }

    @Immutable
    internal data class TradeEntry(
        val profileTradeId: ProfileTradeId,
        val broker: String,
        val ticker: String,
        val side: String,
        val quantity: String,
        val entry: String,
        val exit: String?,
        val duration: String,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
        val fees: String,
    )

    @Immutable
    internal sealed class TradeListItem {

        @Immutable
        internal data class DayHeader(val header: String) : TradeListItem()

        @Immutable
        internal data class Entries(val entries: ImmutableList<TradeEntry>) : TradeListItem()
    }

    @Immutable
    data class ProfileTradeId(
        val profileId: Long,
        val tradeId: Long,
    )
}

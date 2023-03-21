package com.saurabhsandav.core.ui.trades.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.MultipleWindowManager
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

@Immutable
internal data class TradesState(
    val tradesItems: ImmutableList<TradeListItem>,
    val showTradeDetailIds: ImmutableSet<Long>,
    val chartWindowsManager: MultipleWindowManager<TradeChartWindowParams>,
    val fyersLoginWindowState: FyersLoginWindow,
    val bringDetailsToFrontId: Long? = null,
) {

    @Immutable
    internal sealed class FyersLoginWindow {

        @Immutable
        class Open(val fyersLoginState: FyersLoginState) : FyersLoginWindow()

        @Immutable
        object Closed : FyersLoginWindow()
    }

    @Immutable
    internal data class TradeEntry(
        val id: Long,
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
}

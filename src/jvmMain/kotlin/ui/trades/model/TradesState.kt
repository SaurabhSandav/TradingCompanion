package ui.trades.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class TradesState(
    val tradesItems: Map<TradeListItem.DayHeader, List<TradeListItem.Entry>>,
)

@Immutable
internal sealed class TradeListItem {

    @Immutable
    internal data class DayHeader(val header: String) : TradeListItem()

    @Immutable
    internal data class Entry(
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
    ) : TradeListItem()
}

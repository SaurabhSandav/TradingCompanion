package closedtrades

import androidx.compose.runtime.Immutable

@Immutable
internal data class ClosedTradesState(
    val closedTradesItems: List<ClosedTradeListItem.Entry>,
)

@Immutable
internal sealed class ClosedTradeListItem {

    @Immutable
    internal data class DayHeader(val header: String) : ClosedTradeListItem()

    @Immutable
    internal data class Entry(
        val id: Int,
        val broker: String,
        val ticker: String,
        val instrument: String,
        val quantity: String,
        val side: String,
        val entry: String,
        val stop: String,
        val entryTime: String,
        val target: String,
        val exit: String,
        val exitTime: String,
        val pnl: String,
        val netPnl: String,
        val fees: String,
        val duration: String,
        val isProfitable: Boolean,
        val maxFavorableExcursion: String,
        val maxAdverseExcursion: String,
        val persisted: Boolean,
        val persistenceResult: String?,
    ) : ClosedTradeListItem()
}

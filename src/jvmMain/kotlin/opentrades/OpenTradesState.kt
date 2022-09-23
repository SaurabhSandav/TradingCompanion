package opentrades

import androidx.compose.runtime.Immutable

@Immutable
internal data class OpenTradesState(
    val openTrades: List<OpenTradeListEntry>,
)

internal data class OpenTradeListEntry(
    val id: Int,
    val broker: String,
    val ticker: String,
    val instrument: String,
    val quantity: String,
    val side: String,
    val entry: String,
    val stop: String?,
    val entryTime: String,
    val target: String?,
)

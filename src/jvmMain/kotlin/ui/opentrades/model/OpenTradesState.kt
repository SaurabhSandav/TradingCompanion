package ui.opentrades.model

import androidx.compose.runtime.Immutable
import ui.closetradeform.CloseTradeFormWindowParams
import ui.opentradeform.OpenTradeFormWindowParams

@Immutable
internal data class OpenTradesState(
    val openTrades: List<OpenTradeListEntry>,
    val openTradeFormWindowParams: Collection<OpenTradeFormWindowParams>,
    val closeTradeFormWindowParams: Collection<CloseTradeFormWindowParams>,
)

internal data class OpenTradeListEntry(
    val id: Long,
    val broker: String,
    val ticker: String,
    val quantity: String,
    val side: String,
    val entry: String,
    val stop: String,
    val entryTime: String,
    val target: String,
)

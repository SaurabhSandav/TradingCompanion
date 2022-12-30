package ui.opentrades.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateList
import ui.addclosedtrade.CloseTradeWindowState
import ui.opentradeform.OpenTradeFormWindowState

@Immutable
internal data class OpenTradesState(
    val openTrades: List<OpenTradeListEntry>,
    val addTradeWindowStates: SnapshotStateList<OpenTradeFormWindowState>,
    val closeTradeWindowStates: SnapshotStateList<CloseTradeWindowState>,
)

internal data class OpenTradeListEntry(
    val id: Long,
    val broker: String,
    val ticker: String,
    val instrument: String,
    val quantity: String,
    val side: String,
    val entry: String,
    val stop: String,
    val entryTime: String,
    val target: String,
)

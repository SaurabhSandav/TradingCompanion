package ui.sizing.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import ui.addopentrade.AddOpenTradeWindowState

@Immutable
internal data class SizingState(
    val sizedTrades: List<SizedTrade>,
    val addOpenTradeWindowStates: SnapshotStateList<AddOpenTradeWindowState>,
)

@Immutable
internal data class SizedTrade(
    val id: Long,
    val ticker: String,
    val entry: String,
    val stop: String,
    val side: String,
    val spread: String,
    val calculatedQuantity: String,
    val maxAffordableQuantity: String,
    val target: String,
    val color: Color,
)

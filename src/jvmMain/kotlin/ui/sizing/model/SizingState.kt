package ui.sizing.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import ui.opentradeform.OpenTradeFormWindowState

@Immutable
internal data class SizingState(
    val sizedTrades: List<SizedTrade>,
    val openTradeFormWindowStates: SnapshotStateList<OpenTradeFormWindowState>,
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

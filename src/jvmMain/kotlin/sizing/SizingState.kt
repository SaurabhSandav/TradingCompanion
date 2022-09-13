package sizing

import androidx.compose.runtime.Stable

@Stable
internal data class SizingState(
    val sizedTrades: List<SizedTrade>,
)

@Stable
internal data class SizedTrade(
    val ticker: String,
    val entry: String,
    val stop: String,
    val added: String,
    val calculatedQuantity: String,
    val maxAffordableQuantity: String,
    val entryQuantity: String,
    val target: String,
)

@Stable
internal data class PositionSizerTableState(
    val tickerWeight: Float = 2F,
    val entryWeight: Float = 1F,
    val stopWeight: Float = 1F,
    val calculatedQuantityWeight: Float = 1F,
    val maxAffordableQuantityWeight: Float = 1F,
    val entryQuantityWeight: Float = 1F,
    val target1xWeight: Float = 1F,
    val deleteWeight: Float = .5F,
)

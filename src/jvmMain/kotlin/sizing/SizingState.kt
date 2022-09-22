package sizing

import androidx.compose.runtime.Immutable

@Immutable
internal data class SizingState(
    val sizedTrades: List<SizedTrade>,
)

@Immutable
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

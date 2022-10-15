package trading

import kotlinx.datetime.Instant
import java.math.BigDecimal

data class Candle(
    val openInstant: Instant,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val volume: BigDecimal,
)

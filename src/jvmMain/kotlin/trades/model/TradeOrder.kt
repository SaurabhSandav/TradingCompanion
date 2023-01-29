package trades.model

import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

internal data class TradeOrder(
    val id: Long,
    val broker: String,
    val ticker: String,
    val quantity: Int,
    val lots: Int?,
    val type: OrderType,
    val price: BigDecimal,
    val timestamp: LocalDateTime,
)

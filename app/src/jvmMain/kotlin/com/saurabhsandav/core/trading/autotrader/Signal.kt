package com.saurabhsandav.core.trading.autotrader

import java.math.BigDecimal

internal sealed class Signal {

    data object CancelAllEntryOrders : Signal()

    data object ExitAllPositions : Signal()

    data class Buy(
        val price: BigDecimal,
        val stop: BigDecimal,
        val target: BigDecimal,
    ) : Signal()

    data class Sell(
        val price: BigDecimal,
        val stop: BigDecimal,
        val target: BigDecimal,
    ) : Signal()
}

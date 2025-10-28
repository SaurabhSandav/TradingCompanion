package com.saurabhsandav.core.ui.trade

import com.saurabhsandav.core.ui.trade.model.TradeState.TradeStop
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.KMathContext
import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.record.TradeDisplay
import com.saurabhsandav.trading.record.brokerageAt
import com.saurabhsandav.trading.record.model.TradeSide

internal class StopPreviewer(
    private val trade: TradeDisplay,
    private val broker: Broker,
    private val tickSize: KBigDecimal?,
) {

    fun atPrice(price: KBigDecimal): TradeStop? = generateStop(price)

    fun atRisk(risk: KBigDecimal): TradeStop? {

        if (risk <= KBigDecimal.Zero) return null

        val riskPerShare = risk.div(trade.quantity, KMathContext.Decimal32)

        val price = when (trade.side) {
            TradeSide.Long -> trade.averageEntry - riskPerShare
            TradeSide.Short -> trade.averageEntry + riskPerShare
        }

        return generateStop(price)
    }

    private fun generateStop(price: KBigDecimal): TradeStop? {

        val isValid = when (trade.side) {
            TradeSide.Long -> price < trade.averageEntry
            TradeSide.Short -> price > trade.averageEntry
        }

        if (!isValid) return null

        val remainder = tickSize?.let(price::remainder)

        val priceForTickSize = when {
            remainder == null -> price
            remainder.compareTo(KBigDecimal.Zero) == 0 -> price
            else -> when (trade.side) {
                // Stop is rounded up
                TradeSide.Long -> price + (tickSize - remainder)
                // Stop is rounded down
                TradeSide.Short -> price - remainder
            }
        }

        val brokerage = trade.brokerageAt(broker, priceForTickSize)

        return TradeStop(
            price = priceForTickSize,
            priceText = priceForTickSize.toString(),
            risk = brokerage.pnl.negated().toString(),
            netRisk = brokerage.netPNL.toString(),
            isPrimary = false,
        )
    }
}

package com.saurabhsandav.core.ui.trade

import com.saurabhsandav.core.ui.trade.model.TradeState.TradeStop
import com.saurabhsandav.trading.record.TradeDisplay
import com.saurabhsandav.trading.record.brokerageAt
import com.saurabhsandav.trading.record.model.TradeSide
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal class StopPreviewer(
    private val trade: TradeDisplay,
) {

    fun atPrice(price: BigDecimal): TradeStop? = generateStop(price)

    fun atRisk(risk: BigDecimal): TradeStop? {

        if (risk <= BigDecimal.ZERO) return null

        val riskPerShare = risk.divide(trade.quantity, MathContext.DECIMAL32)
            .setScale(2, RoundingMode.HALF_EVEN)

        val price = when (trade.side) {
            TradeSide.Long -> trade.averageEntry - riskPerShare
            TradeSide.Short -> trade.averageEntry + riskPerShare
        }

        return generateStop(price)
    }

    private fun generateStop(price: BigDecimal): TradeStop? {

        val isValid = when (trade.side) {
            TradeSide.Long -> price < trade.averageEntry
            TradeSide.Short -> price > trade.averageEntry
        }

        if (!isValid) return null

        val brokerage = trade.brokerageAt(price)

        fun BigDecimal.strippedPlainText() = stripTrailingZeros().toPlainString()

        return TradeStop(
            price = price,
            priceText = price.strippedPlainText(),
            risk = (brokerage.pnl.negate()).strippedPlainText(),
            netRisk = brokerage.netPNL.strippedPlainText(),
            isPrimary = false,
        )
    }
}

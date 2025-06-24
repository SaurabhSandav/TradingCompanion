package com.saurabhsandav.core.ui.trade

import com.saurabhsandav.core.trading.record.Trade
import com.saurabhsandav.core.trading.record.TradeStop
import com.saurabhsandav.core.trading.record.brokerageAt
import com.saurabhsandav.core.trading.record.model.TradeSide
import com.saurabhsandav.core.trading.record.rValueAt
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTarget
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal class TargetPreviewer(
    private val trade: Trade,
    private val primaryStop: TradeStop?,
) {

    fun atPrice(price: BigDecimal): TradeTarget? = generateTarget(price)

    fun atRValue(rValue: BigDecimal): TradeTarget? {

        if (rValue <= BigDecimal.ZERO || primaryStop?.price == null) return null

        val stopSpread = when (trade.side) {
            TradeSide.Long -> trade.averageEntry - primaryStop.price
            TradeSide.Short -> primaryStop.price - trade.averageEntry
        }

        val profitPerShare = stopSpread.multiply(rValue, MathContext.DECIMAL32)
            .setScale(2, RoundingMode.HALF_EVEN)

        val price = when (trade.side) {
            TradeSide.Long -> trade.averageEntry + profitPerShare
            TradeSide.Short -> trade.averageEntry - profitPerShare
        }

        return generateTarget(price)
    }

    fun atProfit(profit: BigDecimal): TradeTarget? {

        if (profit <= BigDecimal.ZERO) return null

        val profitPerShare = profit.divide(trade.quantity, MathContext.DECIMAL32)
            .setScale(2, RoundingMode.HALF_EVEN)

        val price = when (trade.side) {
            TradeSide.Long -> trade.averageEntry + profitPerShare
            TradeSide.Short -> trade.averageEntry - profitPerShare
        }

        return generateTarget(price)
    }

    private fun generateTarget(price: BigDecimal): TradeTarget? {

        val isValid = when (trade.side) {
            TradeSide.Long -> price > trade.averageEntry
            TradeSide.Short -> price < trade.averageEntry
        }

        if (!isValid) return null

        val brokerage = trade.brokerageAt(price)

        val pnl = when (trade.side) {
            TradeSide.Long -> price - trade.averageEntry
            TradeSide.Short -> trade.averageEntry - price
        }.multiply(trade.quantity, MathContext.DECIMAL32)

        fun BigDecimal.strippedPlainText() = stripTrailingZeros().toPlainString()

        val rValue = primaryStop?.let { trade.rValueAt(pnl, it).toPlainString() }.orEmpty()

        return TradeTarget(
            price = price,
            priceText = price.strippedPlainText(),
            rValue = rValue,
            profit = brokerage.pnl.strippedPlainText(),
            netProfit = brokerage.netPNL.strippedPlainText(),
            isPrimary = false,
        )
    }
}

package com.saurabhsandav.core.ui.trade

import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTarget
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.KMathContext
import com.saurabhsandav.kbigdecimal.KRoundingMode
import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.record.TradeDisplay
import com.saurabhsandav.trading.record.TradeStop
import com.saurabhsandav.trading.record.brokerageAt
import com.saurabhsandav.trading.record.model.TradeSide
import com.saurabhsandav.trading.record.rValueAt

internal class TargetPreviewer(
    private val trade: TradeDisplay,
    private val broker: Broker,
    private val primaryStop: TradeStop?,
) {

    fun atPrice(price: KBigDecimal): TradeTarget? = generateTarget(price)

    fun atRValue(rValue: KBigDecimal): TradeTarget? {

        if (rValue <= KBigDecimal.Zero || primaryStop?.price == null) return null

        val stopSpread = when (trade.side) {
            TradeSide.Long -> trade.averageEntry - primaryStop.price
            TradeSide.Short -> primaryStop.price - trade.averageEntry
        }

        val profitPerShare = stopSpread.times(rValue, KMathContext.Decimal32)
            .decimalPlaces(2, KRoundingMode.HalfEven)

        val price = when (trade.side) {
            TradeSide.Long -> trade.averageEntry + profitPerShare
            TradeSide.Short -> trade.averageEntry - profitPerShare
        }

        return generateTarget(price)
    }

    fun atProfit(profit: KBigDecimal): TradeTarget? {

        if (profit <= KBigDecimal.Zero) return null

        val profitPerShare = profit.div(trade.quantity, KMathContext.Decimal32)

        val price = when (trade.side) {
            TradeSide.Long -> trade.averageEntry + profitPerShare
            TradeSide.Short -> trade.averageEntry - profitPerShare
        }

        return generateTarget(price)
    }

    private fun generateTarget(price: KBigDecimal): TradeTarget? {

        val isValid = when (trade.side) {
            TradeSide.Long -> price > trade.averageEntry
            TradeSide.Short -> price < trade.averageEntry
        }

        if (!isValid) return null

        val brokerage = trade.brokerageAt(broker, price)

        val pnl = when (trade.side) {
            TradeSide.Long -> price - trade.averageEntry
            TradeSide.Short -> trade.averageEntry - price
        }.times(trade.quantity, KMathContext.Decimal32)

        val rValue = primaryStop?.let { trade.rValueAt(pnl, it).toString() }.orEmpty()

        return TradeTarget(
            price = price,
            priceText = price.toString(),
            rValue = rValue,
            profit = brokerage.pnl.toString(),
            netProfit = brokerage.netPNL.toString(),
            isPrimary = false,
        )
    }
}

package com.saurabhsandav.trading.record

import com.saurabhsandav.trading.record.model.TradeSide
import java.math.BigDecimal
import java.math.RoundingMode

fun Trade.brokerageAt(exit: BigDecimal): Brokerage = brokerage(
    broker = broker,
    instrument = instrument,
    entry = averageEntry,
    exit = exit,
    quantity = quantity,
    side = side,
)

fun Trade.brokerageAtExit(): Brokerage? = averageExit?.let(::brokerageAt)

fun Trade.brokerageAt(stop: TradeStop): Brokerage = brokerageAt(stop.price)

fun Trade.brokerageAt(target: TradeTarget): Brokerage = brokerageAt(target.price)

fun Trade.rValueAt(
    pnl: BigDecimal,
    stop: TradeStop,
): BigDecimal = when (side) {
    TradeSide.Long -> pnl.divide((averageEntry - stop.price) * quantity, 4, RoundingMode.HALF_EVEN)
    TradeSide.Short -> pnl.divide((stop.price - averageEntry) * quantity, 4, RoundingMode.HALF_EVEN)
}.setScale(2, RoundingMode.HALF_EVEN).stripTrailingZeros()

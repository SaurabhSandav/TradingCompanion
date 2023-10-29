package com.saurabhsandav.core.trades

import com.saurabhsandav.core.utils.Brokerage
import com.saurabhsandav.core.utils.brokerage
import java.math.BigDecimal

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

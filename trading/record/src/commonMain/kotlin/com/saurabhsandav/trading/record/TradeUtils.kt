package com.saurabhsandav.trading.record

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.KRoundingMode
import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.record.model.TradeSide
import com.saurabhsandav.trading.record.model.isLong

fun Trade.brokerageAt(
    broker: Broker,
    exit: KBigDecimal,
): Brokerage = broker.calculateBrokerage(
    instrument = instrument,
    entry = averageEntry,
    exit = exit,
    quantity = quantity,
    isLong = side.isLong,
)

fun Trade.brokerageAtExit(broker: Broker): Brokerage? {
    return averageExit?.let { exit -> brokerageAt(broker, exit) }
}

fun Trade.brokerageAt(
    broker: Broker,
    stop: TradeStop,
): Brokerage = brokerageAt(broker, stop.price)

fun Trade.brokerageAt(
    broker: Broker,
    target: TradeTarget,
): Brokerage = brokerageAt(broker, target.price)

fun Trade.rValueAt(
    pnl: KBigDecimal,
    stop: TradeStop,
): KBigDecimal = when (side) {
    TradeSide.Long -> pnl.div((averageEntry - stop.price) * quantity, 4, KRoundingMode.HalfEven)
    TradeSide.Short -> pnl.div((stop.price - averageEntry) * quantity, 4, KRoundingMode.HalfEven)
}.decimalPlaces(2, KRoundingMode.HalfEven)

fun TradeDisplay.brokerageAt(
    broker: Broker,
    exit: KBigDecimal,
): Brokerage = broker.calculateBrokerage(
    instrument = instrument,
    entry = averageEntry,
    exit = exit,
    quantity = quantity,
    isLong = side.isLong,
)

fun TradeDisplay.brokerageAtExit(broker: Broker): Brokerage? {
    return averageExit?.let { exit -> brokerageAt(broker, exit) }
}

fun TradeDisplay.brokerageAt(
    broker: Broker,
    stop: TradeStop,
): Brokerage = brokerageAt(broker, stop.price)

fun TradeDisplay.brokerageAt(
    broker: Broker,
    target: TradeTarget,
): Brokerage = brokerageAt(broker, target.price)

fun TradeDisplay.rValueAt(
    pnl: KBigDecimal,
    stop: TradeStop,
): KBigDecimal = when (side) {
    TradeSide.Long -> pnl.div((averageEntry - stop.price) * quantity, 4, KRoundingMode.HalfEven)
    TradeSide.Short -> pnl.div((stop.price - averageEntry) * quantity, 4, KRoundingMode.HalfEven)
}.decimalPlaces(2, KRoundingMode.HalfEven)

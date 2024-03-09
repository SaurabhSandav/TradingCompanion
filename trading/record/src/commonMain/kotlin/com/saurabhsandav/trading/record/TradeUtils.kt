package com.saurabhsandav.trading.record

import com.saurabhsandav.trading.broker.Broker
import com.saurabhsandav.trading.broker.Brokerage
import com.saurabhsandav.trading.record.model.TradeSide
import com.saurabhsandav.trading.record.model.isLong
import java.math.BigDecimal
import java.math.RoundingMode

fun Trade.brokerageAt(
    broker: Broker,
    exit: BigDecimal,
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
    pnl: BigDecimal,
    stop: TradeStop,
): BigDecimal = when (side) {
    TradeSide.Long -> pnl.divide((averageEntry - stop.price) * quantity, 4, RoundingMode.HALF_EVEN)
    TradeSide.Short -> pnl.divide((stop.price - averageEntry) * quantity, 4, RoundingMode.HALF_EVEN)
}.setScale(2, RoundingMode.HALF_EVEN).stripTrailingZeros()

fun TradeDisplay.brokerageAt(
    broker: Broker,
    exit: BigDecimal,
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
    pnl: BigDecimal,
    stop: TradeStop,
): BigDecimal {

    val stopSpread = (averageEntry - stop.price).abs()
    val risk = (stopSpread * quantity).setScale(stopSpread.scale(), RoundingMode.HALF_EVEN)

    return pnl.divide(risk, 4, RoundingMode.HALF_EVEN)
        .setScale(1, RoundingMode.HALF_EVEN)
        .stripTrailingZeros()
}

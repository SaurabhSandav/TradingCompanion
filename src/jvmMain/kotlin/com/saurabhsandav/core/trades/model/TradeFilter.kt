package com.saurabhsandav.core.trades.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import java.math.BigDecimal

data class TradeFilter internal constructor(
    val isClosed: Boolean? = null,
    val side: TradeSide? = null,
    val instantFrom: Instant? = null,
    val instantTo: Instant? = null,
    val timeFrom: LocalTime? = null,
    val timeTo: LocalTime? = null,
    val pnlFrom: BigDecimal? = null,
    val pnlTo: BigDecimal? = null,
    val filterByNetPnl: Boolean = false,
    val hasNotes: Boolean? = null,
) {

    companion object {

        operator fun invoke(block: TradeFilterScope.() -> Unit): TradeFilter = TradeFilter().mutate(block)
    }
}

fun TradeFilter.mutate(block: TradeFilterScope.() -> Unit): TradeFilter {

    val scope = object : TradeFilterScope {

        var filter = this@mutate

        override fun transform(block: (TradeFilter) -> TradeFilter) {
            filter = block(filter)
        }
    }

    scope.block()

    return scope.filter
}

interface TradeFilterScope {

    fun transform(block: (TradeFilter) -> TradeFilter)
}

fun TradeFilterScope.isClosed() {
    transform { it.copy(isClosed = true) }
}

fun TradeFilterScope.isOpen() {
    transform { it.copy(isClosed = false) }
}

fun TradeFilterScope.isLong() {
    transform { it.copy(side = TradeSide.Long) }
}

fun TradeFilterScope.isShort() {
    transform { it.copy(side = TradeSide.Short) }
}

fun TradeFilterScope.hasNotes() {
    transform { it.copy(hasNotes = true) }
}

fun TradeFilterScope.noNotes() {
    transform { it.copy(hasNotes = false) }
}

fun TradeFilterScope.instantRange(
    from: Instant? = null,
    to: Instant? = null,
) {
    transform { it.copy(instantFrom = from, instantTo = to) }
}

fun TradeFilterScope.timeRange(
    from: LocalTime? = null,
    to: LocalTime? = null,
) {
    transform { it.copy(timeFrom = from, timeTo = to) }
}

fun TradeFilterScope.pnlRange(
    from: BigDecimal? = null,
    to: BigDecimal? = null,
    filterByNetPnl: Boolean = false,
) {
    transform { it.copy(pnlFrom = from, pnlTo = to, filterByNetPnl = filterByNetPnl) }
}

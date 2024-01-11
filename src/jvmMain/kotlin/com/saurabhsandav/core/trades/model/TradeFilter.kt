package com.saurabhsandav.core.trades.model

import kotlinx.datetime.Instant

data class TradeFilter internal constructor(
    val isClosed: Boolean? = null,
    val instantFrom: Instant? = null,
    val instantTo: Instant? = null,
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

fun TradeFilterScope.instantRange(
    from: Instant? = null,
    to: Instant? = null,
) {
    transform { it.copy(instantFrom = from, instantTo = to) }
}

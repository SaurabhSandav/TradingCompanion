package com.saurabhsandav.core.ui.trade.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

@Immutable
internal data class TradeState(
    val title: String,
    val details: Details?,
    val orders: ImmutableList<Order>,
    val stops: ImmutableList<TradeStop>,
    val targets: ImmutableList<TradeTarget>,
    val mfeAndMae: MfeAndMae?,
    val notes: ImmutableList<TradeNote>,
    val eventSink: (TradeEvent) -> Unit,
) {

    @Immutable
    internal data class Details(
        val id: Long,
        val broker: String,
        val ticker: String,
        val side: String,
        val quantity: String,
        val entry: String,
        val exit: String?,
        val duration: String,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
        val fees: String,
    )

    @Immutable
    internal data class Order(
        val id: Long,
        val quantity: String,
        val side: String,
        val price: String,
        val timestamp: String,
        val locked: Boolean,
    )

    @Immutable
    internal data class MfeAndMae(
        val mfePrice: String,
        val maePrice: String,
    )

    @Immutable
    internal data class TradeStop(
        val price: BigDecimal,
        val priceText: String,
        val risk: String,
    )

    @Immutable
    internal data class TradeTarget(
        val price: BigDecimal,
        val priceText: String,
        val profit: String,
    )

    @Immutable
    internal data class TradeNote(
        val id: Long,
        val note: String,
        val dateText: String,
    )
}

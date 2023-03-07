package com.saurabhsandav.core.ui.trades.detail.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

@Immutable
internal data class TradeDetailState(
    val tradeDetail: TradeDetail?,
    val stops: ImmutableList<TradeStop>,
    val targets: ImmutableList<TradeTarget>,
) {

    @Immutable
    internal data class TradeDetail(
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
}

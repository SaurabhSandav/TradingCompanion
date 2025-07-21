package com.saurabhsandav.core.ui.trade

import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension
internal interface TradeGraph {

    val presenterFactory: TradePresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(
            @Provides profileTradeId: ProfileTradeId,
        ): TradeGraph
    }
}

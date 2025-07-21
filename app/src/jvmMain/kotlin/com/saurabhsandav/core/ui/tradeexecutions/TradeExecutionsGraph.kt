package com.saurabhsandav.core.ui.tradeexecutions

import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface TradeExecutionsGraph {

    val presenterFactory: TradeExecutionsPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): TradeExecutionsGraph
    }
}

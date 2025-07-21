package com.saurabhsandav.core.ui.tradereview

import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface TradeReviewGraph {

    val presenterFactory: TradeReviewPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(): TradeReviewGraph
    }
}

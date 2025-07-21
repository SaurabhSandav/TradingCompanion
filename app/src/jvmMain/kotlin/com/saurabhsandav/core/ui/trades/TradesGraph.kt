package com.saurabhsandav.core.ui.trades

import com.saurabhsandav.core.trading.ProfileId
import dev.zacsweers.metro.GraphExtension

@GraphExtension
internal interface TradesGraph {

    val presenterFactory: TradesPresenter.Factory

    val profileId: ProfileId

    @GraphExtension.Factory
    interface Factory {

        fun create(): TradesGraph
    }
}

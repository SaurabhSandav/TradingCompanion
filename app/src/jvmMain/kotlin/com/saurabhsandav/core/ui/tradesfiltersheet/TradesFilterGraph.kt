package com.saurabhsandav.core.ui.tradesfiltersheet

import com.saurabhsandav.core.trading.ProfileId
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension
internal interface TradesFilterGraph {

    val presenterFactory: TradesFilterPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(
            @Provides profileId: ProfileId,
        ): TradesFilterGraph
    }
}

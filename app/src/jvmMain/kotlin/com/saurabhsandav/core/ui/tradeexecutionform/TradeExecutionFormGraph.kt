package com.saurabhsandav.core.ui.tradeexecutionform

import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides

@GraphExtension
internal interface TradeExecutionFormGraph {

    val presenterFactory: TradeExecutionFormPresenter.Factory

    @GraphExtension.Factory
    interface Factory {

        fun create(
            @Provides profileId: ProfileId,
            @Provides formType: TradeExecutionFormType,
        ): TradeExecutionFormGraph
    }
}

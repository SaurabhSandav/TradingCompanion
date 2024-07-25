package com.saurabhsandav.core.ui.tradeexecutionform

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import kotlinx.coroutines.CoroutineScope

internal class TradeExecutionFormModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {
            onCloseRequest: () -> Unit,
            profileId: ProfileId,
            formType: TradeExecutionFormType,
        ->

        TradeExecutionFormPresenter(
            onCloseRequest = onCloseRequest,
            coroutineScope = coroutineScope,
            profileId = profileId,
            formType = formType,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}

package com.saurabhsandav.core.ui.tradeexecutionform

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import kotlinx.coroutines.CoroutineScope

internal class TradeExecutionFormModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter: (
        onCloseRequest: () -> Unit,
        ProfileId,
        TradeExecutionFormType,
    ) -> TradeExecutionFormPresenter = { onCloseRequest, profileId, formType ->

        TradeExecutionFormPresenter(
            onCloseRequest = onCloseRequest,
            coroutineScope = coroutineScope,
            profileId = profileId,
            formType = formType,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}

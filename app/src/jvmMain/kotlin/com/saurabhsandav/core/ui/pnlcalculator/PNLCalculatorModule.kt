package com.saurabhsandav.core.ui.pnlcalculator

import com.saurabhsandav.core.di.AppModule
import kotlinx.coroutines.CoroutineScope

internal class PNLCalculatorModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter: () -> PNLCalculatorPresenter = {

        PNLCalculatorPresenter(
            coroutineScope = coroutineScope,
            brokerProvider = appModule.brokerProvider,
        )
    }
}

package com.saurabhsandav.core.ui.account

import com.saurabhsandav.core.AppModule
import kotlinx.coroutines.CoroutineScope

internal class AccountModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {

        AccountPresenter(
            appDispatchers = appModule.appDispatchers,
            coroutineScope = coroutineScope,
            appDB = appModule.appDB,
        )
    }
}

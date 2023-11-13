package com.saurabhsandav.core.ui.barreplay

import com.saurabhsandav.core.AppModule
import kotlinx.coroutines.CoroutineScope

internal class BarReplayModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {

        BarReplayPresenter(
            coroutineScope = coroutineScope,
            appPrefs = appModule.appPrefs,
        )
    }
}

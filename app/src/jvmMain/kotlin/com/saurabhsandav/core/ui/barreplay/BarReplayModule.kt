package com.saurabhsandav.core.ui.barreplay

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.barreplay.session.ReplaySessionModule
import kotlinx.coroutines.CoroutineScope

internal class BarReplayModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter: () -> BarReplayPresenter = {

        BarReplayPresenter(
            coroutineScope = coroutineScope,
            appPrefs = appModule.appPrefs,
        )
    }

    val replaySessionModule: (
        CoroutineScope,
        BarReplayState.ReplayParams,
    ) -> ReplaySessionModule = { coroutineScope, replayParams ->
        ReplaySessionModule(appModule, coroutineScope, replayParams)
    }
}

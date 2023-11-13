package com.saurabhsandav.core.ui.barreplay

import androidx.compose.runtime.Stable
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.barreplay.session.ReplaySessionModule
import kotlinx.coroutines.CoroutineScope

@Stable
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

    val replaySessionModule = {
            coroutineScope: CoroutineScope,
            replayParams: BarReplayState.ReplayParams,
        ->

        ReplaySessionModule(appModule, coroutineScope, replayParams)
    }
}

package com.saurabhsandav.core.ui.tags

import com.saurabhsandav.core.AppModule
import kotlinx.coroutines.CoroutineScope

internal class TagsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {

        TagsPresenter(
            coroutineScope = coroutineScope,
            appPrefs = appModule.appPrefs,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}

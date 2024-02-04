package com.saurabhsandav.core.ui.autotrader

import androidx.compose.runtime.Stable
import com.saurabhsandav.core.di.AppModule
import kotlinx.coroutines.CoroutineScope

@Stable
internal class AutoTraderModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {

        AutoTraderPresenter(
            coroutineScope = coroutineScope,
            appDB = appModule.appDB,
            appPrefs = appModule.appPrefs,
            candleRepo = appModule.candleRepo,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}

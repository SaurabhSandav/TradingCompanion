package com.saurabhsandav.core.ui.studies

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.studies.impl.*
import kotlinx.coroutines.CoroutineScope

internal class StudiesModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
) {

    val presenter = {

        StudiesPresenter(
            coroutineScope = coroutineScope,
            studyFactories = listOf(
                PNLStudy.Factory(
                    appPrefs = appModule.appPrefs,
                    tradingProfiles = appModule.tradingProfiles,
                ),
                PNLByDayStudy.Factory(
                    appPrefs = appModule.appPrefs,
                    tradingProfiles = appModule.tradingProfiles,
                ),
                PNLByDayChartStudy.Factory(
                    appPrefs = appModule.appPrefs,
                    tradingProfiles = appModule.tradingProfiles,
                    webViewStateProvider = appModule.webViewStateProvider,
                ),
                PNLByMonthStudy.Factory(
                    appPrefs = appModule.appPrefs,
                    tradingProfiles = appModule.tradingProfiles,
                ),
                PNLByMonthChartStudy.Factory(
                    appPrefs = appModule.appPrefs,
                    tradingProfiles = appModule.tradingProfiles,
                    webViewStateProvider = appModule.webViewStateProvider,
                ),
                PNLExcursionStudy.Factory(
                    appPrefs = appModule.appPrefs,
                    tradingProfiles = appModule.tradingProfiles,
                ),
                PNLByTickerStudy.Factory(
                    appPrefs = appModule.appPrefs,
                    tradingProfiles = appModule.tradingProfiles,
                ),
                StatsStudy.Factory(
                    appPrefs = appModule.appPrefs,
                    tradingProfiles = appModule.tradingProfiles,
                ),
            ),
        )
    }
}

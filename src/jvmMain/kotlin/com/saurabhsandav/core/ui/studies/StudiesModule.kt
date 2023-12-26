package com.saurabhsandav.core.ui.studies

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.studies.impl.*
import kotlinx.coroutines.CoroutineScope

internal class StudiesModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
) {

    val presenter = {

        StudiesPresenter(
            coroutineScope = coroutineScope,
            studyFactories = listOf(
                PNLStudy.Factory(
                    profileId = profileId,
                    tradingProfiles = appModule.tradingProfiles,
                ),
                PNLByDayStudy.Factory(
                    profileId = profileId,
                    tradingProfiles = appModule.tradingProfiles,
                ),
                PNLByDayChartStudy.Factory(
                    profileId = profileId,
                    tradingProfiles = appModule.tradingProfiles,
                    webViewStateProvider = appModule.webViewStateProvider,
                ),
                PNLByMonthStudy.Factory(
                    profileId = profileId,
                    tradingProfiles = appModule.tradingProfiles,
                ),
                PNLByMonthChartStudy.Factory(
                    profileId = profileId,
                    tradingProfiles = appModule.tradingProfiles,
                    webViewStateProvider = appModule.webViewStateProvider,
                ),
                PNLExcursionStudy.Factory(
                    profileId = profileId,
                    tradingProfiles = appModule.tradingProfiles,
                ),
                PNLByTickerStudy.Factory(
                    profileId = profileId,
                    tradingProfiles = appModule.tradingProfiles,
                ),
                StatsStudy.Factory(
                    profileId = profileId,
                    tradingProfiles = appModule.tradingProfiles,
                ),
            ),
        )
    }
}

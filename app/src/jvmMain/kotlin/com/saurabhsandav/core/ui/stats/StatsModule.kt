package com.saurabhsandav.core.ui.stats

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.stats.studies.PNLByDayChartStudy
import com.saurabhsandav.core.ui.stats.studies.PNLByDayStudy
import com.saurabhsandav.core.ui.stats.studies.PNLByMonthChartStudy
import com.saurabhsandav.core.ui.stats.studies.PNLByMonthStudy
import com.saurabhsandav.core.ui.stats.studies.PNLBySymbolStudy
import com.saurabhsandav.core.ui.stats.studies.PNLExcursionStudy
import com.saurabhsandav.core.ui.stats.studies.PNLStudy
import kotlinx.coroutines.CoroutineScope

internal class StatsModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
) {

    val presenter: () -> StatsPresenter = {

        StatsPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            tradingProfiles = appModule.tradingProfiles,
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
                PNLBySymbolStudy.Factory(
                    profileId = profileId,
                    tradingProfiles = appModule.tradingProfiles,
                ),
            ),
        )
    }
}

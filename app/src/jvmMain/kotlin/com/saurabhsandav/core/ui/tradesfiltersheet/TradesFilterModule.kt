package com.saurabhsandav.core.ui.tradesfiltersheet

import com.saurabhsandav.core.di.AppModule
import com.saurabhsandav.core.trading.record.model.ProfileId
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import kotlinx.coroutines.CoroutineScope

internal class TradesFilterModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
) {

    val presenter: (
        FilterConfig,
        onFilterChange: (FilterConfig) -> Unit,
    ) -> TradesFilterPresenter = { filterConfig, onFilterChange ->

        TradesFilterPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            initialFilterConfig = filterConfig,
            onFilterChange = onFilterChange,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}

package com.saurabhsandav.core.ui.tradesfiltersheet

import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import kotlinx.coroutines.CoroutineScope

internal class TradesFilterModule(
    appModule: AppModule,
    coroutineScope: CoroutineScope,
    profileId: ProfileId,
) {

    val presenter = {
            filterConfig: FilterConfig,
            onFilterChange: (FilterConfig) -> Unit,
        ->

        TradesFilterPresenter(
            coroutineScope = coroutineScope,
            profileId = profileId,
            initialFilterConfig = filterConfig,
            onFilterChange = onFilterChange,
            tradingProfiles = appModule.tradingProfiles,
        )
    }
}

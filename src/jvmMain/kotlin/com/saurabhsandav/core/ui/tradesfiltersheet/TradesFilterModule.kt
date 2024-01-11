package com.saurabhsandav.core.ui.tradesfiltersheet

import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import kotlinx.coroutines.CoroutineScope

internal class TradesFilterModule(
    coroutineScope: CoroutineScope,
) {

    val presenter = {
            filterConfig: FilterConfig,
            onFilterChange: (FilterConfig) -> Unit,
        ->

        TradesFilterPresenter(
            coroutineScope = coroutineScope,
            initialFilterConfig = filterConfig,
            onFilterChange = onFilterChange,
        )
    }
}

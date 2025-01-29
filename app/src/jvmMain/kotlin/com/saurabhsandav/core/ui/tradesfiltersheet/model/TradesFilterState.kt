package com.saurabhsandav.core.ui.tradesfiltersheet.model

import com.saurabhsandav.core.ui.tags.model.TradeTag
import kotlinx.coroutines.flow.Flow

internal data class TradesFilterState(
    val filterConfig: FilterConfig,
    val selectedTags: List<TradeTag>?,
    val tickerSuggestions: (String) -> Flow<List<String>>,
    val eventSink: (TradesFilterEvent) -> Unit,
)

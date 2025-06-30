package com.saurabhsandav.core.ui.tradesfiltersheet.model

import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.coroutines.flow.Flow

internal data class TradesFilterState(
    val filterConfig: FilterConfig,
    val selectedTags: List<TradeTag>?,
    val symbolSuggestions: (String) -> Flow<List<SymbolId>>,
    val eventSink: (TradesFilterEvent) -> Unit,
)

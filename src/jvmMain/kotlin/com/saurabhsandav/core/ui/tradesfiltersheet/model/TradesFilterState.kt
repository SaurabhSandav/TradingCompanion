package com.saurabhsandav.core.ui.tradesfiltersheet.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.TradeTagId
import kotlinx.coroutines.flow.Flow

@Immutable
internal data class TradesFilterState(
    val filterConfig: FilterConfig,
    val selectedTags: List<TradeTag>?,
    val tagSuggestions: (String) -> Flow<List<TradeTag>>,
    val tickerSuggestions: (String) -> Flow<List<String>>,
    val eventSink: (TradesFilterEvent) -> Unit,
) {

    @Immutable
    data class TradeTag(
        val id: TradeTagId,
        val name: String,
        val description: String,
    )
}

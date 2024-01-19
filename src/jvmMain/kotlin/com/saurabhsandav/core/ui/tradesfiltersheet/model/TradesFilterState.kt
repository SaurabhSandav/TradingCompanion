package com.saurabhsandav.core.ui.tradesfiltersheet.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trades.model.TradeTagId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

@Immutable
internal data class TradesFilterState(
    val filterConfig: FilterConfig,
    val selectedTags: ImmutableList<TradeTag>?,
    val tagSuggestions: (String) -> Flow<ImmutableList<TradeTag>>,
    val tickerSuggestions: (String) -> Flow<ImmutableList<String>>,
    val eventSink: (TradesFilterEvent) -> Unit,
) {

    @Immutable
    data class TradeTag(
        val id: TradeTagId,
        val name: String,
        val description: String,
    )
}

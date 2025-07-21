package com.saurabhsandav.core.ui.tradesfiltersheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.DateInterval
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.Notes
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.OpenClosed
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.PNL
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.Side
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.TimeInterval
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.AddSymbol
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.AddTag
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.ApplyFilter
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterByNetPnl
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterDateInterval
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterNotes
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterOpenClosed
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterPnl
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterSide
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.FilterTimeInterval
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.RemoveSymbol
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.RemoveTag
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.ResetFilter
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.SetFilter
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.SetMatchAllTagsEnabled
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterState
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.mapList
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.TradeTagId
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

@AssistedInject
internal class TradesFilterPresenter(
    @Assisted coroutineScope: CoroutineScope,
    profileId: ProfileId,
    @Assisted initialFilterConfig: FilterConfig,
    @Assisted private val onFilterChange: (FilterConfig) -> Unit,
    private val tradingProfiles: TradingProfiles,
) {

    private val tradingRecord = coroutineScope.async { tradingProfiles.getRecord(profileId) }
    private var filterConfig by mutableStateOf(initialFilterConfig)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradesFilterState(
            filterConfig = filterConfig,
            selectedTags = getSelectedTags(),
            symbolSuggestions = ::symbolSuggestions,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradesFilterEvent) {

        when (event) {
            is SetFilter -> onSetFilter(event.filterConfig)
            is FilterOpenClosed -> onFilterOpenClosed(event.openClosed)
            is FilterSide -> onFilterSide(event.side)
            is FilterDateInterval -> onFilterDateInterval(event.dateInterval)
            is FilterTimeInterval -> onFilterTimeInterval(event.timeInterval)
            is FilterPnl -> onFilterPnl(event.pnl)
            is FilterByNetPnl -> onFilterByNetPnl(event.isEnabled)
            is FilterNotes -> onFilterNotes(event.notes)
            is AddTag -> onAddTag(event.id)
            is RemoveTag -> onRemoveTag(event.id)
            is SetMatchAllTagsEnabled -> onSetMatchAllTagsEnabled(event.isEnabled)
            is AddSymbol -> onAddSymbol(event.symbolId)
            is RemoveSymbol -> onRemoveSymbol(event.symbolId)
            ResetFilter -> onResetFilter()
            ApplyFilter -> onApplyFilter()
        }
    }

    @Composable
    private fun getSelectedTags(): List<TradeTag>? {

        // Information of whether a filter is enabled is required when first composing UI.
        // Sending an empty list followed by the actual fetch means the UI initially thinks there are no tags selected.
        // Send a null initially if tags not selected. In this case, an empty list will signify tags are loading.
        val initial: List<TradeTag>? = remember {
            if (filterConfig.tags.isEmpty()) null else emptyList()
        }

        return produceState(initial) {

            snapshotFlow { filterConfig.tags }
                .flatMapLatest { tradingRecord.await().tags.getByIds(it) }
                .mapList { tag ->

                    TradeTag(
                        id = tag.id,
                        name = tag.name,
                        description = tag.description.ifBlank { null },
                        color = tag.color?.let(::Color),
                    )
                }
                .collect { value = it }
        }.value
    }

    private fun symbolSuggestions(filterQuery: String): Flow<List<SymbolId>> = flow {

        snapshotFlow { filterConfig.symbols }
            .flatMapLatest { symbolIds ->
                tradingRecord.await().trades.getSuggestedSymbols(
                    query = filterQuery,
                    ignore = symbolIds,
                )
            }
            .emitInto(this)
    }

    private fun onSetFilter(filterConfig: FilterConfig) {
        this.filterConfig = filterConfig
    }

    private fun onFilterOpenClosed(openClosed: OpenClosed) {
        filterConfig = filterConfig.copy(openClosed = openClosed)
    }

    private fun onFilterSide(side: Side) {
        filterConfig = filterConfig.copy(side = side)
    }

    private fun onFilterDateInterval(dateInterval: DateInterval) {
        filterConfig = filterConfig.copy(dateInterval = dateInterval)
    }

    private fun onFilterTimeInterval(timeInterval: TimeInterval) {
        filterConfig = filterConfig.copy(timeInterval = timeInterval)
    }

    private fun onFilterPnl(pnl: PNL) {
        filterConfig = filterConfig.copy(pnl = pnl)
    }

    private fun onFilterByNetPnl(isEnabled: Boolean) {
        filterConfig = filterConfig.copy(filterByNetPnl = isEnabled)
    }

    private fun onFilterNotes(notes: Notes) {
        filterConfig = filterConfig.copy(notes = notes)
    }

    private fun onAddTag(id: TradeTagId) {
        filterConfig = filterConfig.copy(tags = filterConfig.tags + id)
    }

    private fun onRemoveTag(id: TradeTagId) {
        filterConfig = filterConfig.copy(tags = filterConfig.tags - id)
    }

    private fun onSetMatchAllTagsEnabled(isEnabled: Boolean) {
        filterConfig = filterConfig.copy(matchAllTags = isEnabled)
    }

    private fun onAddSymbol(symbolId: SymbolId) {
        filterConfig = filterConfig.copy(symbols = filterConfig.symbols + symbolId)
    }

    private fun onRemoveSymbol(symbolId: SymbolId) {
        filterConfig = filterConfig.copy(symbols = filterConfig.symbols - symbolId)
    }

    private fun onResetFilter() {
        filterConfig = FilterConfig()
    }

    private fun onApplyFilter() {
        onFilterChange(filterConfig)
    }

    @AssistedFactory
    fun interface Factory {

        fun create(
            coroutineScope: CoroutineScope,
            initialFilterConfig: FilterConfig,
            onFilterChange: (FilterConfig) -> Unit,
        ): TradesFilterPresenter
    }
}

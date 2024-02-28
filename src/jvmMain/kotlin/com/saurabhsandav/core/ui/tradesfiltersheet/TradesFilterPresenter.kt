package com.saurabhsandav.core.ui.tradesfiltersheet

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeTagId
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.*
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.*
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterState
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterState.TradeTag
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.mapList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

internal class TradesFilterPresenter(
    coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    initialFilterConfig: FilterConfig,
    private val onFilterChange: (FilterConfig) -> Unit,
    private val tradingProfiles: TradingProfiles,
) {

    private var filterConfig by mutableStateOf(initialFilterConfig)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradesFilterState(
            filterConfig = filterConfig,
            selectedTags = getSelectedTags(),
            tagSuggestions = ::tagSuggestions,
            tickerSuggestions = ::tickerSuggestions,
            eventSink = ::onEvent,
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

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
            is AddTicker -> onAddTicker(event.ticker)
            is RemoveTicker -> onRemoveTicker(event.ticker)
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

            val tradesRepo = tradingProfiles.getRecord(profileId).trades

            snapshotFlow { filterConfig.tags }
                .flatMapLatest(tradesRepo::getTagsByIds)
                .mapList { tag ->

                    TradeTag(
                        id = tag.id,
                        name = tag.name,
                        description = tag.description,
                    )
                }
                .collect { value = it }
        }.value
    }

    private fun tagSuggestions(filterQuery: String): Flow<List<TradeTag>> = flow {

        val tradesRepo = tradingProfiles.getRecord(profileId).trades

        snapshotFlow { filterConfig.tags }
            .flatMapLatest { tagIds ->
                tradesRepo.getSuggestedTags(
                    query = filterQuery,
                    ignoreIds = tagIds,
                )
            }
            .mapList { tag ->

                TradeTag(
                    id = tag.id,
                    name = tag.name,
                    description = tag.description,
                )
            }
            .emitInto(this)
    }

    private fun tickerSuggestions(filterQuery: String): Flow<List<String>> = flow {

        val tradesRepo = tradingProfiles.getRecord(profileId).trades

        snapshotFlow { filterConfig.tickers }
            .flatMapLatest { tickers ->
                tradesRepo.getSuggestedTickers(
                    query = filterQuery,
                    ignore = tickers,
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

    private fun onAddTicker(ticker: String) {
        filterConfig = filterConfig.copy(tickers = filterConfig.tickers + ticker)
    }

    private fun onRemoveTicker(ticker: String) {
        filterConfig = filterConfig.copy(tickers = filterConfig.tickers - ticker)
    }

    private fun onResetFilter() {
        filterConfig = FilterConfig()
    }

    private fun onApplyFilter() {
        onFilterChange(filterConfig)
    }
}

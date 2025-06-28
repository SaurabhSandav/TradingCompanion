package com.saurabhsandav.core.ui.tickerselectiondialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.TickersProvider
import com.saurabhsandav.core.ui.tickerselectiondialog.model.TickerSelectionEvent
import com.saurabhsandav.core.ui.tickerselectiondialog.model.TickerSelectionEvent.Filter
import com.saurabhsandav.core.ui.tickerselectiondialog.model.TickerSelectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

internal class TickerSelectionPresenter(
    coroutineScope: CoroutineScope,
    private val tickersProvider: TickersProvider,
) {

    private var filterQuery by mutableStateOf("")

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TickerSelectionState(
            tickers = getTickers(),
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TickerSelectionEvent) {

        when (event) {
            is Filter -> onFilter(event.query)
        }
    }

    @Composable
    private fun getTickers(): Flow<PagingData<String>> = remember {

        val pagingConfig = PagingConfig(
            pageSize = 100,
            enablePlaceholders = false,
            maxSize = 1000,
        )

        snapshotFlow { filterQuery }.flatMapLatest { filterQuery ->

            Pager(
                config = pagingConfig,
                pagingSourceFactory = { tickersProvider.getTickers(filterQuery) },
            ).flow
        }
    }

    private fun onFilter(query: String) {
        filterQuery = query
    }
}

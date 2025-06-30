package com.saurabhsandav.core.ui.symbolselectiondialog

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
import com.saurabhsandav.core.trading.SymbolsProvider
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionEvent
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionEvent.Filter
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionState
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

internal class SymbolSelectionPresenter(
    coroutineScope: CoroutineScope,
    private val symbolsProvider: SymbolsProvider,
) {

    private var filterQuery by mutableStateOf("")

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule SymbolSelectionState(
            symbols = getSymbols(),
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: SymbolSelectionEvent) {

        when (event) {
            is Filter -> onFilter(event.query)
        }
    }

    @Composable
    private fun getSymbols(): Flow<PagingData<SymbolId>> = remember {

        val pagingConfig = PagingConfig(
            pageSize = 100,
            enablePlaceholders = false,
            maxSize = 1000,
        )

        snapshotFlow { filterQuery }.flatMapLatest { filterQuery ->

            Pager(
                config = pagingConfig,
                pagingSourceFactory = { symbolsProvider.getSymbols(filterQuery) },
            ).flow
        }
    }

    private fun onFilter(query: String) {
        filterQuery = query
    }
}

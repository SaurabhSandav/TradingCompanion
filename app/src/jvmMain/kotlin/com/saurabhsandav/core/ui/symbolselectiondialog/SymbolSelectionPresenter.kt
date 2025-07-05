package com.saurabhsandav.core.ui.symbolselectiondialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.SymbolsProvider
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionEvent
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionEvent.Filter
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionEvent.SymbolSelected
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionState
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionState.Symbol
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal class SymbolSelectionPresenter(
    coroutineScope: CoroutineScope,
    initialFilterQuery: String,
    private val symbolsProvider: SymbolsProvider,
    initialSelectedSymbolId: SymbolId? = null,
) {

    private var selectedSymbolId by mutableStateOf(initialSelectedSymbolId)
    private var filterQuery by mutableStateOf(TextFieldValue(initialFilterQuery, TextRange(initialFilterQuery.length)))

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule SymbolSelectionState(
            symbols = getSymbols(),
            selectedSymbol = getSelectedSymbol(),
            filterQuery = filterQuery,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: SymbolSelectionEvent) {

        when (event) {
            is SymbolSelected -> onSymbolSelected(event.id)
            is Filter -> onFilter(event.query)
        }
    }

    @Composable
    private fun getSymbols(): Flow<PagingData<Symbol>> = remember {

        val pagingConfig = PagingConfig(
            pageSize = 100,
            enablePlaceholders = false,
            maxSize = 1000,
        )

        snapshotFlow { filterQuery.text }
            .flatMapLatest { filterQuery ->

                Pager(
                    config = pagingConfig,
                    pagingSourceFactory = { symbolsProvider.getSymbols(filterQuery) },
                ).flow
            }
            .map { pagingData ->
                pagingData.map { symbol ->
                    Symbol(
                        id = symbol,
                        ticker = symbol.value,
                    )
                }
            }
    }

    @Composable
    private fun getSelectedSymbol(): Symbol? = produceState<Symbol?>(null) {

        snapshotFlow { selectedSymbolId }.collect { id ->

            value = when (id) {
                null -> null
                else -> Symbol(
                    id = id,
                    ticker = id.value,
                )
            }
        }
    }.value

    private fun onSymbolSelected(id: SymbolId) {
        selectedSymbolId = id
    }

    private fun onFilter(query: TextFieldValue) {
        filterQuery = query
    }
}

package com.saurabhsandav.core.ui.symbolselectiondialog

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.SymbolsProvider
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionEvent
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionEvent.SymbolSelected
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionState
import com.saurabhsandav.core.ui.symbolselectiondialog.model.SymbolSelectionState.Symbol
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.market.india.FinvasiaBroker
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@AssistedInject
internal class SymbolSelectionPresenter(
    @Assisted coroutineScope: CoroutineScope,
    @Assisted initialFilterQuery: String,
    private val symbolsProvider: SymbolsProvider,
    @Assisted initialSelectedSymbolId: SymbolId? = null,
) {

    private var selectedSymbolId by mutableStateOf(initialSelectedSymbolId)
    private val filterQuery = TextFieldState(initialFilterQuery)

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
        }
    }

    @Composable
    private fun getSymbols(): Flow<PagingData<Symbol>> = remember {

        val pagingConfig = PagingConfig(
            pageSize = 100,
            enablePlaceholders = false,
            maxSize = 1000,
        )

        snapshotFlow { filterQuery.text.toString() }
            .onStart { symbolsProvider.downloadAllLatestSymbols() }
            .flatMapLatest { filterQuery ->

                Pager(
                    config = pagingConfig,
                    pagingSourceFactory = {
                        symbolsProvider.getSymbolsFiltered(
                            filterQuery = filterQuery,
                            instruments = listOf(Instrument.Index, Instrument.Equity),
                            exchange = "NSE",
                        )
                    },
                ).flow
            }
            .map { pagingData ->
                pagingData.map { cachedSymbol ->
                    Symbol(
                        id = cachedSymbol.id,
                        ticker = cachedSymbol.ticker,
                    )
                }
            }
    }

    @Composable
    private fun getSelectedSymbol(): Symbol? = produceState<Symbol?>(null) {

        snapshotFlow { selectedSymbolId }
            .flatMapLatest { id ->
                when (id) {
                    null -> flowOf(null)
                    else -> symbolsProvider.getSymbol(FinvasiaBroker.Id, id)
                }
            }
            .collect { cachedSymbol ->

                value = when (cachedSymbol) {
                    null -> null
                    else -> Symbol(
                        id = cachedSymbol.id,
                        ticker = cachedSymbol.ticker,
                    )
                }
            }
    }.value

    private fun onSymbolSelected(id: SymbolId) {
        selectedSymbolId = id
    }

    @AssistedFactory
    interface Factory {

        fun create(
            coroutineScope: CoroutineScope,
            initialFilterQuery: String,
            initialSelectedSymbolId: SymbolId? = null,
        ): SymbolSelectionPresenter
    }
}

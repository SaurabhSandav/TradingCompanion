package com.saurabhsandav.core.ui.tickerselectiondialog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.ui.tickerselectiondialog.model.TickerSelectionEvent
import com.saurabhsandav.core.ui.tickerselectiondialog.model.TickerSelectionEvent.Filter
import com.saurabhsandav.core.ui.tickerselectiondialog.model.TickerSelectionState
import kotlinx.coroutines.CoroutineScope

internal class TickerSelectionPresenter(
    coroutineScope: CoroutineScope,
    private val tickers: List<String>,
) {

    private var currentTickers by mutableStateOf(tickers)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TickerSelectionState(
            tickers = currentTickers,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TickerSelectionEvent) {

        when (event) {
            is Filter -> onFilter(event.query)
        }
    }

    private fun onFilter(query: String) {

        currentTickers = when {
            query.isBlank() -> tickers
            else -> tickers.filter { it.contains(query, ignoreCase = true) }
        }
    }
}

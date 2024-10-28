package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.trades.model.TradesEvent.*

internal class TradesLandingSwitcherItem(
    private val tradesModule: TradesModule,
) : LandingSwitcherItem {

    private val presenter = tradesModule.presenter()

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TradesScreen(
            profileId = tradesModule.profileId,
            tradeEntries = state.tradeEntries,
            isFocusModeEnabled = state.isFocusModeEnabled,
            selectionManager = state.selectionManager,
            onOpenDetails = { state.eventSink(OpenDetails(it)) },
            onOpenChart = { state.eventSink(OpenChart(it)) },
            onSetFocusModeEnabled = { state.eventSink(SetFocusModeEnabled(it)) },
            onApplyFilter = { state.eventSink(ApplyFilter(it)) },
            onNewExecution = { state.eventSink(NewExecution) },
            onDeleteTrades = { ids -> state.eventSink(DeleteTrades(ids)) },
            tagSuggestions = state.tagSuggestions,
            onAddTag = { tradeIds, tagId -> state.eventSink(AddTag(tradeIds, tagId)) },
            errors = state.errors,
        )
    }
}

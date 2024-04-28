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
            onOpenDetails = { state.eventSink(OpenDetails(it)) },
            onOpenChart = { state.eventSink(OpenChart(it)) },
            onSetFocusModeEnabled = { state.eventSink(SetFocusModeEnabled(it)) },
            onApplyFilter = { state.eventSink(ApplyFilter(it)) },
            errors = state.errors,
        )
    }
}

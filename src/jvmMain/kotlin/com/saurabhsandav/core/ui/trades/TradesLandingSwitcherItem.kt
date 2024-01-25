package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenDetails

internal class TradesLandingSwitcherItem(
    tradesModule: TradesModule,
) : LandingSwitcherItem {

    private val presenter = tradesModule.presenter()

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TradesScreen(
            tradesList = state.tradesList,
            onOpenDetails = { state.eventSink(OpenDetails(it)) },
            onOpenChart = { state.eventSink(OpenChart(it)) },
            errors = state.errors,
        )
    }
}

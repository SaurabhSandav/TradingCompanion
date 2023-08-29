package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.landing.model.LandingState.TradeWindowParams
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenDetails
import kotlinx.coroutines.CoroutineScope

internal class TradesLandingSwitcherItem(
    coroutineScope: CoroutineScope,
    appModule: AppModule,
    tradeWindowsManager: AppWindowsManager<TradeWindowParams>,
) : LandingSwitcherItem {

    private val presenter = TradesPresenter(coroutineScope, appModule, tradeWindowsManager)

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TradesScreen(
            tradesItems = state.tradesItems,
            onOpenDetails = { state.eventSink(OpenDetails(it)) },
            onOpenChart = { state.eventSink(OpenChart(it)) },
            errors = state.errors,
        )
    }

    @Composable
    override fun Windows() {

        val state by presenter.state.collectAsState()

        TradesScreenWindows(
            chartWindowsManager = state.chartWindowsManager,
            fyersLoginWindowState = state.fyersLoginWindowState,
        )
    }
}

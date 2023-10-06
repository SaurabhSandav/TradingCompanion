package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.TradeContentLauncher
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenDetails
import kotlinx.coroutines.CoroutineScope

internal class TradesLandingSwitcherItem(
    coroutineScope: CoroutineScope,
    appModule: AppModule,
    tradeContentLauncher: TradeContentLauncher,
) : LandingSwitcherItem {

    private val presenter = TradesPresenter(coroutineScope, appModule, tradeContentLauncher)

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TradesScreen(
            tradesByDays = state.tradesByDays,
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
        )
    }
}

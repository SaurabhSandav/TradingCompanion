package com.saurabhsandav.core.ui.sizing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.*
import kotlinx.coroutines.CoroutineScope

internal class SizingLandingSwitcherItem(
    coroutineScope: CoroutineScope,
    appModule: AppModule,
) : LandingSwitcherItem {

    private val presenter = SizingPresenter(coroutineScope, appModule)

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        SizingScreen(
            sizedTrades = state.sizedTrades,
            onUpdateEntry = { id, entry -> presenter.event(UpdateTradeEntry(id, entry)) },
            onUpdateStop = { id, stop -> presenter.event(UpdateTradeStop(id, stop)) },
            onOpenLiveTrade = { id -> presenter.event(OpenLiveTrade(id)) },
            onDeleteTrade = { id -> presenter.event(RemoveTrade(id)) },
            onAddTrade = { ticker -> presenter.event(AddTrade(ticker)) },
        )
    }

    @Composable
    override fun Windows() {

        val state by presenter.state.collectAsState()

        SizingScreenWindows(
            orderFormWindowsManager = state.orderFormWindowsManager,
        )
    }
}

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
            onUpdateEntry = { id, entry -> state.eventSink(UpdateTradeEntry(id, entry)) },
            onUpdateStop = { id, stop -> state.eventSink(UpdateTradeStop(id, stop)) },
            onOpenLiveTrade = { id -> state.eventSink(OpenLiveTrade(id)) },
            onDeleteTrade = { id -> state.eventSink(RemoveTrade(id)) },
            onAddTrade = { ticker -> state.eventSink(AddTrade(ticker)) },
        )
    }

    @Composable
    override fun Windows() {

        val state by presenter.state.collectAsState()

        SizingScreenWindows(
            executionFormWindowsManager = state.executionFormWindowsManager,
        )
    }
}

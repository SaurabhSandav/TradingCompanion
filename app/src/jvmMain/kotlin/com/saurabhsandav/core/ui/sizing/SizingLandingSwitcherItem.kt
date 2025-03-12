package com.saurabhsandav.core.ui.sizing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.AddTrade
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.OpenLiveTrade
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.RemoveTrade
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.UpdateTradeEntry
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.UpdateTradeStop

internal class SizingLandingSwitcherItem(
    sizingModule: SizingModule,
) : LandingSwitcherItem {

    private val presenter = sizingModule.presenter()

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

        SizingScreenWindows(
            executionFormWindowsManager = presenter.executionFormWindowsManager,
        )
    }
}

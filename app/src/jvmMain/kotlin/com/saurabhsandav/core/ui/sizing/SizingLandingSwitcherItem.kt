package com.saurabhsandav.core.ui.sizing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingGraph
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.landing.LandingSwitcherItemKey
import com.saurabhsandav.core.ui.landing.model.LandingScreen
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.AddTrade
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.OpenLiveTrade
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.RemoveTrade
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.UpdateTradeEntry
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.UpdateTradeStop
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope

@LandingSwitcherItemKey(LandingScreen.TradeSizing)
@ContributesIntoMap(LandingGraph::class)
@Inject
internal class SizingLandingSwitcherItem(
    landingGraph: LandingGraph,
    coroutineScope: CoroutineScope,
) : LandingSwitcherItem {

    private val graph = landingGraph.sizingGraphFactory.create()

    private val presenter = graph.presenterFactory.create(coroutineScope)

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        SizingScreen(
            sizedTrades = state.sizedTrades,
            onUpdateEntry = { id, entry -> state.eventSink(UpdateTradeEntry(id, entry)) },
            onUpdateStop = { id, stop -> state.eventSink(UpdateTradeStop(id, stop)) },
            onOpenLiveTrade = { id -> state.eventSink(OpenLiveTrade(id)) },
            onDeleteTrade = { id -> state.eventSink(RemoveTrade(id)) },
            onAddTrade = { symbolId -> state.eventSink(AddTrade(symbolId)) },
        )
    }

    @Composable
    override fun Windows() {

        SizingScreenWindows(
            executionFormWindowsManager = presenter.executionFormWindowsManager,
        )
    }
}

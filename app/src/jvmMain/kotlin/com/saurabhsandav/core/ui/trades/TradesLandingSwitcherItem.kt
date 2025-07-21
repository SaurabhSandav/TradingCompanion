package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingGraph
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.landing.LandingSwitcherItemKey
import com.saurabhsandav.core.ui.landing.model.LandingScreen
import com.saurabhsandav.core.ui.trades.model.TradesEvent.AddTag
import com.saurabhsandav.core.ui.trades.model.TradesEvent.ApplyFilter
import com.saurabhsandav.core.ui.trades.model.TradesEvent.DeleteTrades
import com.saurabhsandav.core.ui.trades.model.TradesEvent.NewExecution
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenDetails
import com.saurabhsandav.core.ui.trades.model.TradesEvent.SetFocusModeEnabled
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope

@LandingSwitcherItemKey(LandingScreen.Trades)
@ContributesIntoMap(LandingGraph::class)
@Inject
internal class TradesLandingSwitcherItem(
    landingGraph: LandingGraph,
    coroutineScope: CoroutineScope,
) : LandingSwitcherItem {

    private val graph = landingGraph.tradesGraphFactory.create()

    private val presenter = graph.presenterFactory.create(coroutineScope)

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TradesScreen(
            profileId = graph.profileId,
            tradeEntries = state.tradeEntries,
            isFocusModeEnabled = state.isFocusModeEnabled,
            selectionManager = state.selectionManager,
            onOpenDetails = { state.eventSink(OpenDetails(it)) },
            onOpenChart = { state.eventSink(OpenChart(it)) },
            onSetFocusModeEnabled = { state.eventSink(SetFocusModeEnabled(it)) },
            onApplyFilter = { state.eventSink(ApplyFilter(it)) },
            onNewExecution = { state.eventSink(NewExecution) },
            onDeleteTrades = { ids -> state.eventSink(DeleteTrades(ids)) },
            onAddTag = { tradeIds, tagId -> state.eventSink(AddTag(tradeIds, tagId)) },
        )
    }
}

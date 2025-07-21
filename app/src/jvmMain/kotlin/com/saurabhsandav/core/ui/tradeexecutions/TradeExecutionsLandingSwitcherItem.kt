package com.saurabhsandav.core.ui.tradeexecutions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingGraph
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.landing.LandingSwitcherItemKey
import com.saurabhsandav.core.ui.landing.model.LandingScreen
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.DeleteExecutions
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.EditExecution
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.LockExecutions
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.NewExecution
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.NewExecutionFromExisting
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope

@LandingSwitcherItemKey(LandingScreen.TradeExecutions)
@ContributesIntoMap(LandingGraph::class)
@Inject
internal class TradeExecutionsLandingSwitcherItem(
    landingGraph: LandingGraph,
    coroutineScope: CoroutineScope,
) : LandingSwitcherItem {

    private val graph = landingGraph.tradeExecutionsGraphFactory.create()

    private val presenter = graph.presenterFactory.create(coroutineScope)

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TradeExecutionsScreen(
            onNewExecution = { state.eventSink(NewExecution) },
            executionEntries = state.executionEntries,
            selectionManager = state.selectionManager,
            onNewExecutionFromExisting = { state.eventSink(NewExecutionFromExisting(it)) },
            onLockExecutions = { ids -> state.eventSink(LockExecutions(ids)) },
            onEditExecution = { state.eventSink(EditExecution(it)) },
            onDeleteExecutions = { ids -> state.eventSink(DeleteExecutions(ids)) },
        )
    }
}

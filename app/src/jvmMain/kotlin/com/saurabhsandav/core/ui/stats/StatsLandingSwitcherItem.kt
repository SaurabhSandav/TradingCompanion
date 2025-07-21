package com.saurabhsandav.core.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingGraph
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.landing.LandingSwitcherItemKey
import com.saurabhsandav.core.ui.landing.model.LandingScreen
import com.saurabhsandav.core.ui.stats.model.StatsEvent.OpenStudy
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope

@LandingSwitcherItemKey(LandingScreen.Stats)
@ContributesIntoMap(LandingGraph::class)
@Inject
internal class StatsLandingSwitcherItem(
    landingGraph: LandingGraph,
    coroutineScope: CoroutineScope,
) : LandingSwitcherItem {

    private val graph = landingGraph.statsGraphFactory.create()

    private val presenter = graph.presenterFactory.create(coroutineScope)

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        StatsScreen(
            statsCategories = state.statsCategories,
            studyFactories = state.studyFactories,
            onOpenStudy = { studyFactory -> state.eventSink(OpenStudy(studyFactory)) },
        )
    }

    @Composable
    override fun Windows() {

        StatsScreenWindows(
            studyWindowsManager = presenter.studyWindowsManager,
        )
    }
}

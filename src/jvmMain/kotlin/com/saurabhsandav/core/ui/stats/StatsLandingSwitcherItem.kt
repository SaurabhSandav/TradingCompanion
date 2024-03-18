package com.saurabhsandav.core.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.stats.model.StatsEvent.OpenStudy

internal class StatsLandingSwitcherItem(
    statsModule: StatsModule,
) : LandingSwitcherItem {

    private val presenter = statsModule.presenter()

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

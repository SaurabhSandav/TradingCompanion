package com.saurabhsandav.core.ui.studies

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.studies.model.StudiesEvent

internal class StudiesLandingSwitcherItem(
    studiesModule: StudiesModule,
) : LandingSwitcherItem {

    private val presenter = studiesModule.presenter()

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        StudiesScreen(
            studyFactories = state.studyFactories,
            onOpenStudy = { studyFactory -> state.eventSink(StudiesEvent.OpenStudy(studyFactory)) },
        )
    }

    @Composable
    override fun Windows() {

        val state by presenter.state.collectAsState()

        StudiesScreenWindows(
            studyWindowsManager = state.studyWindowsManager,
        )
    }
}

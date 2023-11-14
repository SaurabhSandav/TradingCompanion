package com.saurabhsandav.core.ui.studies

import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.studies.impl.Study
import com.saurabhsandav.core.ui.studies.model.StudiesEvent
import com.saurabhsandav.core.ui.studies.model.StudiesState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope

@Stable
internal class StudiesPresenter(
    coroutineScope: CoroutineScope,
    private val studyFactories: List<Study.Factory<out Study>>,
) {

    val studyWindowsManager = AppWindowsManager<Study.Factory<*>>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule StudiesState(
            studyFactories = remember { studyFactories.toImmutableList() },
            studyWindowsManager = studyWindowsManager,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: StudiesEvent) {

        when (event) {
            is StudiesEvent.OpenStudy -> onOpenStudy(event.studyFactory)
        }
    }

    private fun onOpenStudy(studyFactory: Study.Factory<*>) {

        val window = studyWindowsManager.windows.find { it.params == studyFactory }

        when (window) {

            // Open new window
            null -> studyWindowsManager.newWindow(studyFactory)

            // Window already open. Bring to front.
            else -> window.toFront()
        }
    }
}

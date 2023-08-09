package com.saurabhsandav.core.ui.studies

import androidx.compose.runtime.Stable
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.studies.impl.*
import com.saurabhsandav.core.ui.studies.model.StudiesEvent
import com.saurabhsandav.core.ui.studies.model.StudiesState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow

@Stable
internal class StudiesPresenter(
    coroutineScope: CoroutineScope,
    appModule: AppModule,
) {

    private val events = MutableSharedFlow<StudiesEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val studyFactories = persistentListOf(
        PNLStudy.Factory(appModule),
        PNLByDayStudy.Factory(appModule),
        PNLByDayChartStudy.Factory(appModule),
        PNLByMonthStudy.Factory(appModule),
        PNLByMonthChartStudy.Factory(appModule),
        PNLExcursionStudy.Factory(appModule),
        PNLByTickerStudy.Factory(appModule),
        StatsStudy.Factory(appModule),
    )

    private val studyWindowsManager = AppWindowsManager<Study.Factory<*>>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is StudiesEvent.OpenStudy -> onOpenStudy(event.studyFactory)
            }
        }

        return@launchMolecule StudiesState(
            studyFactories = studyFactories,
            studyWindowsManager = studyWindowsManager,
        )
    }

    fun event(event: StudiesEvent) {
        events.tryEmit(event)
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

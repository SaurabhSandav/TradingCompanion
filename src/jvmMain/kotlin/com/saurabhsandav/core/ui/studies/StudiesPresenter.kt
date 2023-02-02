package com.saurabhsandav.core.ui.studies

import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.studies.*
import kotlinx.coroutines.CoroutineScope

internal class StudiesPresenter(
    coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        val studies = remember {
            listOf(
                PNLStudy.Factory(appModule),
                PNLByDayStudy.Factory(appModule),
                PNLByDayChartStudy.Factory(appModule),
                PNLByMonthStudy.Factory(appModule),
                PNLByMonthChartStudy.Factory(appModule),
                PNLExcursionStudy.Factory(appModule),
                PNLByTickerStudy.Factory(appModule),
            )
        }

        return@launchMolecule StudiesState(
            studyFactories = studies,
        )
    }
}

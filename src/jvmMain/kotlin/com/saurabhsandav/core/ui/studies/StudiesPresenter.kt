package com.saurabhsandav.core.ui.studies

import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.studies.impl.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope

@Stable
internal class StudiesPresenter(
    coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        val studies = remember {
            persistentListOf(
                PNLStudy.Factory(appModule),
                PNLByDayStudy.Factory(appModule),
                PNLByDayChartStudy.Factory(appModule),
                PNLByMonthStudy.Factory(appModule),
                PNLByMonthChartStudy.Factory(appModule),
                PNLExcursionStudy.Factory(appModule),
                PNLByTickerStudy.Factory(appModule),
                StatsStudy.Factory(appModule),
            )
        }

        return@launchMolecule StudiesState(
            studyFactories = studies,
        )
    }
}

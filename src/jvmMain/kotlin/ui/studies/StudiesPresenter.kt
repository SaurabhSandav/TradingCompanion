package ui.studies

import AppModule
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import studies.*

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
                TickerChartStudy.Factory(appModule),
                BarReplayStudy.Factory(appModule),
            )
        }

        return@launchMolecule StudiesState(
            studyFactories = studies,
        )
    }
}

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
                PNLStudy(appModule),
                PNLByDayStudy(appModule),
                PNLByDayChartStudy(appModule),
                PNLByMonthStudy(appModule),
                PNLByMonthChartStudy(appModule),
                PNLExcursionStudy(appModule),
                PNLByTickerStudy(appModule),
                TickerChartStudy(appModule),
            )
        }

        return@launchMolecule StudiesState(
            studies = studies,
        )
    }
}

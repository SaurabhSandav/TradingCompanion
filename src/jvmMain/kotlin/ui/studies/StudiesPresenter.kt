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
                PNLByMonthStudy(appModule),
                PNLByDayChartStudy(appModule),
                PNLExcursionStudy(appModule),
                PNLByTickerStudy(appModule),
            )
        }

        return@launchMolecule StudiesState(
            studies = studies,
        )
    }
}

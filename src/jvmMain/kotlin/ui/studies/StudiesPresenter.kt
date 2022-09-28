package ui.studies

import AppModule
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import studies.PNLExcursionStudy
import studies.PNLStudy

internal class StudiesPresenter(
    coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    val state = coroutineScope.launchMolecule(RecompositionClock.Immediate) {

        val studies = remember {
            listOf(
                PNLStudy(appModule),
                PNLExcursionStudy(appModule),
            )
        }

        return@launchMolecule StudiesState(
            studies = studies,
        )
    }
}

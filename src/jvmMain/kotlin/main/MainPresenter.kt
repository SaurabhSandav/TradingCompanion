package main

import AppModule
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

internal class MainPresenter(
    private val appModule: AppModule,
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob() as CoroutineContext)

    val state = coroutineScope.launchMolecule(RecompositionClock.Immediate) {
        return@launchMolecule MainState()
    }
}

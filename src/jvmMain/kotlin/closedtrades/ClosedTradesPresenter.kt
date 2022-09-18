package closedtrades

import Account
import AppModule
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.GetAllClosedTradesDetailed
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mapList
import java.math.BigDecimal
import kotlin.coroutines.CoroutineContext

internal class ClosedTradesPresenter(
    private val appModule: AppModule,
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob() as CoroutineContext)

    val state = coroutineScope.launchMolecule(RecompositionClock.Immediate) {

        val account by appModule.account.collectAsState(
            Account(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
            )
        )

        val closedTradesDetailed by remember {
            appModule.appDB.closedTradeQueries
                .getAllClosedTradesDetailed()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .mapList { it.toClosedTradeDetailed() }
        }.collectAsState(emptyList())

        return@launchMolecule ClosedTradesState(
            closedTradesDetailed = closedTradesDetailed
        )
    }

    private fun GetAllClosedTradesDetailed.toClosedTradeDetailed(): ClosedTradeDetailed {
        return ClosedTradeDetailed(
            id = id,
            date = date,
            broker = broker,
            ticker = ticker,
            instrument = instrument,
            quantity = quantity,
            side = side,
            entry = entry,
            stop = stop,
            entryTime = entryTime,
            target = target,
            exit = exit,
            exitTime = exitTime,
            maxFavorableExcursion = maxFavorableExcursion,
            maxAdverseExcursion = maxAdverseExcursion,
            persisted = persisted,
            persistenceResult = persistenceResult,
        )
    }
}

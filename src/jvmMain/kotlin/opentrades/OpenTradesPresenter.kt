package opentrades

import AppModule
import Side
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.OpenTrade
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import mapList

internal class OpenTradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
) {

    val state = coroutineScope.launchMolecule(RecompositionClock.Immediate) {

        val closedTradesEntries by remember {
            appModule.appDB.openTradeQueries
                .getAll()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .mapList { openTrade -> openTrade.toOpenTradeListEntry() }
        }.collectAsState(emptyList())

        return@launchMolecule OpenTradesState(
            openTrades = closedTradesEntries,
        )
    }

    fun addTrade(
        ticker: String,
        quantity: String,
        isLong: Boolean,
        entry: String,
        stop: String,
        target: String,
    ) {

        appModule.appDB.openTradeQueries.insert(
            broker = "Finvasia",
            ticker = ticker,
            instrument = "equity",
            quantity = quantity,
            lots = null,
            side = (if (isLong) Side.Long else Side.Short).strValue,
            entry = entry,
            stop = stop,
            entryDate = "",
            target = target,
        )
    }

    private fun OpenTrade.toOpenTradeListEntry(): OpenTradeListEntry {

        val entryDateTime = LocalDateTime.parse(entryDate)

        return OpenTradeListEntry(
            id = id,
            broker = broker,
            ticker = ticker,
            instrument = instrument,
            quantity = lots?.let { "$quantity ($it ${if (it == 1) "lot" else "lots"})" } ?: quantity,
            side = this@toOpenTradeListEntry.side,
            entry = entry,
            stop = stop,
            entryTime = entryDateTime.time.toString(),
            target = target,
        )
    }
}

package ui.addclosedtradedetailed

import com.saurabhsandav.core.AppDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import launchUnit
import model.Side

internal class CloseTradeDetailedWindowState(
    private val appDB: AppDB,
    val formModel: CloseTradeDetailedFormFields.Model,
    private val coroutineScope: CoroutineScope,
    val onCloseRequest: () -> Unit,
) {

    fun onSaveTrade(model: CloseTradeDetailedFormFields.Model) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {

            appDB.transaction {

                appDB.closedTradeQueries.insert(
                    id = model.id,
                    broker = "Finvasia",
                    ticker = model.ticker!!,
                    instrument = "equity",
                    quantity = model.quantity,
                    lots = null,
                    side = (if (model.isLong) Side.Long else Side.Short).strValue,
                    entry = model.entry,
                    stop = model.stop.ifBlank { null },
                    entryDate = model.entryDateTime.toString(),
                    target = model.target.ifBlank { null },
                    exit = model.exit,
                    exitDate = model.exitDateTime.toString(),
                )

                appDB.closedTradeDetailQueries.insert(
                    closedTradeId = model.id,
                    maxFavorableExcursion = model.maxFavorableExcursion.ifBlank { null },
                    maxAdverseExcursion = model.maxAdverseExcursion.ifBlank { null },
                    tags = model.tags.joinToString(", "),
                    persisted = model.persisted.toString(),
                    persistenceResult = null,
                )
            }
        }

        onCloseRequest()
    }
}

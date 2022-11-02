package ui.addclosedtrade

import com.saurabhsandav.core.AppDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Side

internal class CloseTradeWindowState(
    private val appDB: AppDB,
    val formModel: CloseTradeFormFields.Model,
    private val coroutineScope: CoroutineScope,
    val onCloseRequest: () -> Unit,
) {

    fun onSaveTrade(model: CloseTradeFormFields.Model) = coroutineScope.launch {

        withContext(Dispatchers.IO) {

            appDB.transaction {

                appDB.closedTradeQueries.insert(
                    id = null,
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

                appDB.openTradeQueries.delete(model.id)
            }
        }

        onCloseRequest()
    }
}

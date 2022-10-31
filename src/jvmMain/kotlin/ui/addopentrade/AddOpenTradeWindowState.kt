package ui.addopentrade

import com.saurabhsandav.core.AppDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import launchUnit
import model.Side

internal class AddOpenTradeWindowState(
    private val appDB: AppDB,
    val formModel: AddOpenTradeFormFields.Model,
    private val coroutineScope: CoroutineScope,
    val onCloseRequest: () -> Unit,
) {

    fun onSaveTrade(model: AddOpenTradeFormFields.Model) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {

            appDB.openTradeQueries.insert(
                id = model.id,
                broker = "Finvasia",
                ticker = model.ticker!!,
                instrument = "equity",
                quantity = model.quantity,
                lots = null,
                side = (if (model.isLong) Side.Long else Side.Short).strValue,
                entry = model.entry,
                stop = model.stop,
                entryDate = model.entryDateTime.toString(),
                target = model.target,
            )
        }

        onCloseRequest()
    }
}

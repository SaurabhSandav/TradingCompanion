package ui.closetradeform

import AppModule
import LocalAppModule
import androidx.compose.runtime.*
import com.saurabhsandav.core.AppDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import model.Side
import ui.closetradeform.CloseTradeFormWindowParams.OperationType.CloseOpenTrade
import ui.closetradeform.CloseTradeFormWindowParams.OperationType.EditExistingTrade
import kotlin.time.Duration.Companion.nanoseconds

@Composable
internal fun rememberCloseTradeFormWindowState(
    params: CloseTradeFormWindowParams,
): CloseTradeFormWindowState {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current

    return remember {
        CloseTradeFormWindowState(
            params = params,
            coroutineScope = scope,
            appModule = appModule,
        )
    }
}

internal class CloseTradeFormWindowState(
    val params: CloseTradeFormWindowParams,
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appDB: AppDB = appModule.appDB,
) {

    var isReady by mutableStateOf(false)
        private set

    var model by mutableStateOf(
        CloseTradeFormFields.Model(
            id = -1,
            ticker = null,
            quantity = "",
            isLong = true,
            entry = "",
            stop = "",
            entryDateTime = run {
                val currentTime = Clock.System.now()
                val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds
                currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
            },
            target = "",
            exit = "",
            exitDateTime = run {
                val currentTime = Clock.System.now()
                val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds
                currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
            },
        )
    )

    init {

        coroutineScope.launch {

            model = when (params.operationType) {
                is EditExistingTrade -> editExistingTrade(params.operationType.id)
                is CloseOpenTrade -> closeOpenTrade(params.operationType.openTradeId)
            }

            isReady = true
        }
    }

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

                if (params.operationType is CloseOpenTrade) {
                    appDB.openTradeQueries.delete(model.id)
                }
            }
        }

        params.onCloseRequest()
    }

    private suspend fun editExistingTrade(id: Long): CloseTradeFormFields.Model {

        val closedTrade = withContext(Dispatchers.IO) {
            appDB.closedTradeQueries.getClosedTradesDetailedById(id).executeAsOne()
        }

        return CloseTradeFormFields.Model(
            id = closedTrade.id,
            ticker = closedTrade.ticker,
            quantity = closedTrade.quantity,
            isLong = Side.fromString(closedTrade.side) == Side.Long,
            entry = closedTrade.entry,
            stop = closedTrade.stop.orEmpty(),
            entryDateTime = LocalDateTime.parse(closedTrade.entryDate),
            target = closedTrade.target.orEmpty(),
            exit = closedTrade.exit,
            exitDateTime = LocalDateTime.parse(closedTrade.exitDate),
        )
    }

    private suspend fun closeOpenTrade(openTradeId: Long): CloseTradeFormFields.Model {

        val openTrade = withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.getById(openTradeId).executeAsOne()
        }

        val currentTime = Clock.System.now()
        val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds

        // TODO Fetch current price
        // val accessToken = appPrefs.getString(PrefKeys.FyersAccessToken)
        // val response = fyersApi.getQuotes(accessToken, listOf("NSE:${openTrade.ticker}-EQ"))
        // val currentPrice = response.result?.quote?.first()?.quoteData?.cmd?.close?.toString() ?: "0"

        return CloseTradeFormFields.Model(
            id = openTrade.id,
            ticker = openTrade.ticker,
            quantity = openTrade.quantity,
            isLong = Side.fromString(openTrade.side) == Side.Long,
            entry = openTrade.entry,
            stop = openTrade.stop.orEmpty(),
            entryDateTime = LocalDateTime.parse(openTrade.entryDate),
            target = openTrade.target.orEmpty(),
            exit = "",
            exitDateTime = currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault()),
        )
    }
}

internal class CloseTradeFormWindowParams(
    val operationType: OperationType,
    val onCloseRequest: () -> Unit,
) {

    sealed class OperationType {

        data class CloseOpenTrade(val openTradeId: Long) : OperationType()

        data class EditExistingTrade(val id: Long) : OperationType()
    }
}

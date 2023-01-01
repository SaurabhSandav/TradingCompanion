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
import launchUnit
import model.Side
import ui.closetradeform.CloseTradeFormWindowParams.OperationType.CloseOpenTrade
import ui.closetradeform.CloseTradeFormWindowParams.OperationType.EditExistingTrade
import ui.common.form.FormValidator
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

    private val formValidator = FormValidator()

    var isReady by mutableStateOf(false)
        private set

    var showDetails by mutableStateOf(false)
        private set

    val model = CloseTradeFormModel(
        validator = formValidator,
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

    var detailModel: CloseTradeDetailFormModel? = null

    init {

        coroutineScope.launch {

            when (params.operationType) {
                is EditExistingTrade -> editExistingTrade(params.operationType.id)
                is CloseOpenTrade -> closeOpenTrade(params.operationType.openTradeId)
            }

            isReady = true
        }
    }

    fun onSaveTrade() = coroutineScope.launchUnit {

        if (!formValidator.isValid()) return@launchUnit

        withContext(Dispatchers.IO) {

            appDB.transaction {

                val id = when (params.operationType) {
                    is CloseOpenTrade -> null
                    is EditExistingTrade -> params.operationType.id
                }

                appDB.closedTradeQueries.insert(
                    id = id,
                    broker = "Finvasia",
                    ticker = model.ticker.value!!,
                    instrument = "equity",
                    quantity = model.quantity.value,
                    lots = null,
                    side = (if (model.isLong.value) Side.Long else Side.Short).strValue,
                    entry = model.entry.value,
                    stop = model.stop.value.ifBlank { null },
                    entryDate = model.entryDateTime.value.toString(),
                    target = model.target.value.ifBlank { null },
                    exit = model.exit.value,
                    exitDate = model.exitDateTime.value.toString(),
                )

                if (id != null) {

                    val detailModel = requireNotNull(detailModel)

                    appDB.closedTradeDetailQueries.insert(
                        closedTradeId = id,
                        maxFavorableExcursion = detailModel.maxFavorableExcursion.value.ifBlank { null },
                        maxAdverseExcursion = detailModel.maxAdverseExcursion.value.ifBlank { null },
                        tags = detailModel.tags.joinToString(", "),
                        persisted = detailModel.persisted.toString(),
                        persistenceResult = null,
                    )
                }

                if (params.operationType is CloseOpenTrade) {
                    appDB.openTradeQueries.delete(params.operationType.openTradeId)
                }
            }
        }

        params.onCloseRequest()
    }

    fun showDetails() {
        showDetails = true
    }

    private suspend fun editExistingTrade(id: Long) {

        val closedTrade = withContext(Dispatchers.IO) {
            appDB.closedTradeQueries.getClosedTradesDetailedById(id).executeAsOne()
        }

        detailModel = CloseTradeDetailFormModel(
            validator = formValidator,
            closeTradeFormModel = model,
            maxFavorableExcursion = closedTrade.maxFavorableExcursion.orEmpty(),
            maxAdverseExcursion = closedTrade.maxAdverseExcursion.orEmpty(),
            tags = closedTrade.tags?.split(", ")?.let {
                if (it.size == 1 && it.first().isBlank()) emptyList() else it
            } ?: emptyList(),
            persisted = closedTrade.persisted.toBoolean(),
        )

        model.ticker.value = closedTrade.ticker
        model.quantity.value = closedTrade.quantity
        model.isLong.value = Side.fromString(closedTrade.side) == Side.Long
        model.entry.value = closedTrade.entry
        model.stop.value = closedTrade.stop.orEmpty()
        model.entryDateTime.value = LocalDateTime.parse(closedTrade.entryDate)
        model.target.value = closedTrade.target.orEmpty()
        model.exit.value = closedTrade.exit
        model.exitDateTime.value = LocalDateTime.parse(closedTrade.exitDate)
    }

    private suspend fun closeOpenTrade(openTradeId: Long) {

        val openTrade = withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.getById(openTradeId).executeAsOne()
        }

        val currentTime = Clock.System.now()
        val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds

        // TODO Fetch current price
        // val accessToken = appPrefs.getString(PrefKeys.FyersAccessToken)
        // val response = fyersApi.getQuotes(accessToken, listOf("NSE:${openTrade.ticker}-EQ"))
        // val currentPrice = response.result?.quote?.first()?.quoteData?.cmd?.close?.toString() ?: "0"

        model.ticker.value = openTrade.ticker
        model.quantity.value = openTrade.quantity
        model.isLong.value = Side.fromString(openTrade.side) == Side.Long
        model.entry.value = openTrade.entry
        model.stop.value = openTrade.stop.orEmpty()
        model.entryDateTime.value = LocalDateTime.parse(openTrade.entryDate)
        model.target.value = openTrade.target.orEmpty()
        model.exit.value = ""
        model.exitDateTime.value = currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
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

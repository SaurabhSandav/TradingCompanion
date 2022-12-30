package ui.opentradeform

import AppModule
import LocalAppModule
import androidx.compose.runtime.*
import com.saurabhsandav.core.AppDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import launchUnit
import model.Side
import ui.opentradeform.OpenTradeFormWindowParams.OperationType.*
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.time.Duration.Companion.nanoseconds

@Composable
internal fun rememberOpenTradeFormWindowState(
    params: OpenTradeFormWindowParams,
): OpenTradeFormWindowState {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current

    return remember {
        OpenTradeFormWindowState(
            params = params,
            coroutineScope = scope,
            appModule = appModule,
        )
    }
}

internal class OpenTradeFormWindowState(
    val params: OpenTradeFormWindowParams,
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appDB: AppDB = appModule.appDB,
) {

    var isReady by mutableStateOf(false)
        private set

    var model by mutableStateOf(
        OpenTradeFormFields.Model(
            id = null,
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
        )
    )

    init {

        coroutineScope.launch {

            model = when (params.operationType) {
                New -> model
                is EditExisting -> editExistingTrade(params.operationType.id)
                is OpenFromSizingTrade -> openFromSizingTrade(params.operationType.sizingTradeId)
            }

            isReady = true
        }
    }

    fun onSaveTrade(model: OpenTradeFormFields.Model) = coroutineScope.launchUnit {

        withContext(Dispatchers.IO) {

            appDB.transaction {

                appDB.openTradeQueries.insert(
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
                )

                if (params.operationType is OpenFromSizingTrade) {
                    appDB.sizingTradeQueries.delete(params.operationType.sizingTradeId)
                }
            }
        }

        params.onCloseRequest()
    }

    private suspend fun editExistingTrade(id: Long): OpenTradeFormFields.Model {

        val openTrade = withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.getById(id).executeAsOne()
        }

        return OpenTradeFormFields.Model(
            id = openTrade.id,
            ticker = openTrade.ticker,
            quantity = openTrade.quantity,
            isLong = Side.fromString(openTrade.side) == Side.Long,
            entry = openTrade.entry,
            stop = openTrade.stop.orEmpty(),
            entryDateTime = LocalDateTime.parse(openTrade.entryDate),
            target = openTrade.target.orEmpty(),
        )
    }

    private suspend fun openFromSizingTrade(sizingTradeId: Long): OpenTradeFormFields.Model {

        val sizingTrade = withContext(Dispatchers.IO) {
            appModule.appDB.sizingTradeQueries.get(sizingTradeId).executeAsOne()
        }

        val entryBD = sizingTrade.entry.toBigDecimal()
        val stopBD = sizingTrade.stop.toBigDecimal()

        val entryStopComparison = entryBD.compareTo(stopBD)

        val isLong = when {
            // Short
            entryStopComparison < 0 -> false
            // Long (even if entry and stop are the same). Form should validate before saving.
            else -> true
        }

        val spread = (entryBD - stopBD).abs()
        val account = appModule.account.first()

        val calculatedQuantity = when {
            spread.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.riskAmount / spread).setScale(0, RoundingMode.FLOOR)
        }

        val maxAffordableQuantity = when {
            entryBD.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.balancePerTrade * account.leverage) / entryBD
        }

        val currentTime = Clock.System.now()
        val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds

        return OpenTradeFormFields.Model(
            id = null,
            ticker = sizingTrade.ticker,
            quantity = calculatedQuantity.min(maxAffordableQuantity).toPlainString(),
            isLong = isLong,
            entry = sizingTrade.entry,
            stop = sizingTrade.stop,
            entryDateTime = currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault()),
            target = when {
                entryBD > stopBD -> entryBD + spread // Long
                else -> entryBD - spread // Short
            }.toPlainString()
        )
    }
}

internal class OpenTradeFormWindowParams(
    val operationType: OperationType,
    val onCloseRequest: () -> Unit,
) {

    sealed class OperationType {

        object New : OperationType()

        data class EditExisting(val id: Long) : OperationType()

        data class OpenFromSizingTrade(val sizingTradeId: Long) : OperationType()
    }
}

package com.saurabhsandav.core.ui.opentradeform

import androidx.compose.runtime.*
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.launchUnit
import com.saurabhsandav.core.model.Side
import com.saurabhsandav.core.ui.common.form.FormValidator
import com.saurabhsandav.core.ui.opentradeform.OpenTradeFormWindowParams.OperationType.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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

    private val formValidator = FormValidator()

    var isReady by mutableStateOf(false)
        private set

    val model = OpenTradeFormModel(
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
    )

    init {

        coroutineScope.launch {

            when (params.operationType) {
                is EditExisting -> editExistingTrade(params.operationType.id)
                is OpenFromSizingTrade -> openFromSizingTrade(params.operationType.sizingTradeId)
                New -> Unit
            }

            isReady = true
        }
    }

    fun onSaveTrade() = coroutineScope.launchUnit {

        if (!formValidator.isValid()) return@launchUnit

        withContext(Dispatchers.IO) {

            appDB.transaction {

                appDB.openTradeQueries.insert(
                    id = if (params.operationType is EditExisting) params.operationType.id else null,
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
                )

                if (params.operationType is OpenFromSizingTrade) {
                    appDB.sizingTradeQueries.delete(params.operationType.sizingTradeId)
                }
            }
        }

        params.onCloseRequest()
    }

    private suspend fun editExistingTrade(id: Long) {

        val openTrade = withContext(Dispatchers.IO) {
            appModule.appDB.openTradeQueries.getById(id).executeAsOne()
        }

        model.ticker.value = openTrade.ticker
        model.quantity.value = openTrade.quantity
        model.isLong.value = Side.fromString(openTrade.side) == Side.Long
        model.entry.value = openTrade.entry
        model.stop.value = openTrade.stop.orEmpty()
        model.entryDateTime.value = LocalDateTime.parse(openTrade.entryDate)
        model.target.value = openTrade.target.orEmpty()
    }

    private suspend fun openFromSizingTrade(sizingTradeId: Long) {

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

        model.ticker.value = sizingTrade.ticker
        model.quantity.value = calculatedQuantity.min(maxAffordableQuantity).toPlainString()
        model.isLong.value = isLong
        model.entry.value = sizingTrade.entry
        model.stop.value = sizingTrade.stop
        model.entryDateTime.value = currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault())
        model.target.value = when {
            entryBD > stopBD -> entryBD + spread // Long
            else -> entryBD - spread // Short
        }.toPlainString()
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

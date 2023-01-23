package ui.sizing

import AppModule
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.SizingTrade
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import fyers_api.FyersApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import launchUnit
import mapList
import model.Account
import model.Side
import ui.common.AppColor
import ui.common.CollectEffect
import ui.opentradeform.OpenTradeFormWindowParams
import ui.sizing.model.SizedTrade
import ui.sizing.model.SizingEvent
import ui.sizing.model.SizingEvent.*
import ui.sizing.model.SizingState
import utils.PrefKeys
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

internal class SizingPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val fyersApi: FyersApi = appModule.fyersApiFactory(),
) {

    private val events = MutableSharedFlow<SizingEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val openTradeFormWindowParams = mutableStateMapOf<UUID, OpenTradeFormWindowParams>()

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is AddTrade -> addTrade(event.ticker)
                is UpdateTradeEntry -> updateTradeEntry(event.id, event.entry)
                is UpdateTradeStop -> updateTradeStop(event.id, event.stop)
                is OpenTrade -> openTrade(event.id)
                is RemoveTrade -> removeTrade(event.id)
            }
        }

        val account by appModule.account.collectAsState(
            Account(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
            )
        )

        return@launchMolecule SizingState(
            sizedTrades = getSizedTrades(account),
            openTradeFormWindowParams = openTradeFormWindowParams.values,
        )
    }

    fun event(event: SizingEvent) {
        events.tryEmit(event)
    }

    private fun addTrade(ticker: String) = coroutineScope.launchUnit(Dispatchers.IO) {

        val accessToken = appPrefs.getStringOrNull(PrefKeys.FyersAccessToken)
        val currentPrice = accessToken?.let {
            val response = fyersApi.getQuotes(accessToken, listOf("NSE:$ticker-EQ"))
            response.result?.quote?.first()?.quoteData?.cmd?.close?.toString()
        } ?: "0"

        appModule.appDB.sizingTradeQueries.insert(
            id = null,
            ticker = ticker,
            entry = currentPrice,
            stop = currentPrice,
        )
    }

    private fun updateTradeEntry(id: Long, entry: String) {

        if (entry.toBigDecimalOrNull() == null) return

        coroutineScope.launch(Dispatchers.IO) {
            appModule.appDB.sizingTradeQueries.updateEntry(entry = entry, id = id)
        }
    }

    private fun updateTradeStop(id: Long, stop: String) {

        if (stop.toBigDecimalOrNull() == null) return

        coroutineScope.launch(Dispatchers.IO) {
            appModule.appDB.sizingTradeQueries.updateStop(stop = stop, id = id)
        }
    }

    private fun openTrade(id: Long) {

        // Don't allow opening duplicate windows
        val isWindowAlreadyOpen = openTradeFormWindowParams.values.any {
            (it.operationType as OpenTradeFormWindowParams.OperationType.OpenFromSizingTrade).sizingTradeId == id
        }
        if (isWindowAlreadyOpen) return

        val key = UUID.randomUUID()

        openTradeFormWindowParams[key] = OpenTradeFormWindowParams(
            operationType = OpenTradeFormWindowParams.OperationType.OpenFromSizingTrade(id),
            onCloseRequest = { openTradeFormWindowParams.remove(key) }
        )
    }

    private fun removeTrade(id: Long) = coroutineScope.launchUnit(Dispatchers.IO) {
        appModule.appDB.sizingTradeQueries.delete(id)
    }

    @Composable
    private fun getSizedTrades(account: Account): List<SizedTrade> {
        return remember(account) {
            appModule.appDB.sizingTradeQueries
                .getAll()
                .asFlow()
                .mapToList(Dispatchers.IO)
                .mapList { sizingTrade -> sizingTrade.size(account) }
        }.collectAsState(emptyList()).value
    }

    private fun SizingTrade.size(account: Account): SizedTrade {

        val entryBD = entry.toBigDecimal()
        val stopBD = stop.toBigDecimal()

        val entryStopComparison = entryBD.compareTo(stopBD)

        val spread = (entryBD - stopBD).abs()

        val calculatedQuantity = when {
            spread.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.riskAmount / spread).setScale(0, RoundingMode.FLOOR)
        }

        val maxAffordableQuantity = when {
            entryBD.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.balancePerTrade * account.leverage) / entryBD
        }

        return SizedTrade(
            id = id,
            ticker = ticker,
            entry = entry,
            stop = stop,
            side = when {
                entryStopComparison > 0 -> Side.Long.strValue
                entryStopComparison < 0 -> Side.Short.strValue
                else -> ""
            }.uppercase(),
            spread = spread.toPlainString(),
            calculatedQuantity = calculatedQuantity.toPlainString(),
            maxAffordableQuantity = maxAffordableQuantity.toPlainString(),
            target = when {
                entryBD > stopBD -> entryBD + spread // Long
                else -> entryBD - spread // Short
            }.toPlainString(),
            color = when {
                entryStopComparison > 0 -> AppColor.ProfitGreen
                entryStopComparison < 0 -> AppColor.LossRed
                else -> Color.Transparent
            },
        )
    }
}

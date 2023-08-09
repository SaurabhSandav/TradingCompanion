package com.saurabhsandav.core.ui.sizing

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.SizingTrade
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.Account
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.sizing.model.SizingEvent
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.*
import com.saurabhsandav.core.ui.sizing.model.SizingState
import com.saurabhsandav.core.ui.sizing.model.SizingState.OrderFormParams
import com.saurabhsandav.core.ui.sizing.model.SizingState.SizedTrade
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormModel
import com.saurabhsandav.core.ui.tradeorderform.model.OrderFormType
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.time.Duration.Companion.nanoseconds

@Stable
internal class SizingPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
) {

    private val events = MutableSharedFlow<SizingEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val orderFormWindowsManager = AppWindowsManager<OrderFormParams>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is AddTrade -> addTrade(event.ticker)
                is UpdateTradeEntry -> updateTradeEntry(event.id, event.entry)
                is UpdateTradeStop -> updateTradeStop(event.id, event.stop)
                is OpenLiveTrade -> openLiveTrade(event.id)
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
            orderFormWindowsManager = orderFormWindowsManager,
        )
    }

    fun event(event: SizingEvent) {
        events.tryEmit(event)
    }

    private fun addTrade(ticker: String) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.currentRecord.first()

        tradingRecord.sizingTrades.new(
            ticker = ticker,
            entry = 100.toBigDecimal(),
            stop = 90.toBigDecimal(),
        )
    }

    private fun updateTradeEntry(id: Long, entry: String) = coroutineScope.launchUnit {

        val entryBD = entry.toBigDecimalOrNull() ?: return@launchUnit

        val tradingRecord = tradingProfiles.currentRecord.first()

        tradingRecord.sizingTrades.updateEntry(
            id = id,
            entry = entryBD,
        )
    }

    private fun updateTradeStop(id: Long, stop: String) = coroutineScope.launchUnit {

        val stopBD = stop.toBigDecimalOrNull() ?: return@launchUnit

        val tradingRecord = tradingProfiles.currentRecord.first()

        tradingRecord.sizingTrades.updateStop(
            id = id,
            stop = stopBD,
        )
    }

    private fun removeTrade(id: Long) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.currentRecord.first()

        tradingRecord.sizingTrades.delete(id)
    }

    private fun openLiveTrade(id: Long) = coroutineScope.launchUnit {

        val tradingRecord = tradingProfiles.currentRecord.first()

        val sizingTrade = tradingRecord.sizingTrades.getById(id).first()

        val entryStopComparison = sizingTrade.entry.compareTo(sizingTrade.stop)

        val isBuy = when {
            // Short
            entryStopComparison < 0 -> false
            // Long (even if entry and stop are the same). Form should validate before saving.
            else -> true
        }

        val spread = (sizingTrade.entry - sizingTrade.stop).abs()
        val account = appModule.account.first()

        val calculatedQuantity = when {
            spread.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.riskAmount / spread).setScale(0, RoundingMode.FLOOR)
        }

        val maxAffordableQuantity = when {
            sizingTrade.entry.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.balancePerTrade * account.leverage) / sizingTrade.entry
        }

        val currentTime = Clock.System.now()
        val currentTimeWithoutNanoseconds = currentTime - currentTime.nanosecondsOfSecond.nanoseconds

        val params = OrderFormParams(
            id = UUID.randomUUID(),
            profileId = tradingProfiles.currentProfile.first().id,
            formType = OrderFormType.New { formValidator ->
                OrderFormModel(
                    validator = formValidator,
                    instrument = Instrument.Equity.strValue,
                    ticker = sizingTrade.ticker,
                    quantity = calculatedQuantity.min(maxAffordableQuantity).toPlainString(),
                    lots = "",
                    isBuy = isBuy,
                    price = sizingTrade.entry.toPlainString(),
                    timestamp = currentTimeWithoutNanoseconds.toLocalDateTime(TimeZone.currentSystemDefault()),
                )
            },
            onOrderSaved = { orderId ->

                coroutineScope.launch {

                    // Single order can close a trade and open a new one.
                    // Make sure to choose the open trade
                    val trade = tradingRecord.trades.getTradesForOrder(orderId).first().single { !it.isClosed }

                    // Add stop
                    tradingRecord.trades.addStop(trade.id, sizingTrade.stop)

                    val target = when (trade.side) {
                        TradeSide.Long -> sizingTrade.entry + spread
                        TradeSide.Short -> sizingTrade.entry - spread
                    }

                    // Add target
                    tradingRecord.trades.addTarget(trade.id, target)
                }
            },
        )

        orderFormWindowsManager.newWindow(params)
    }

    @Composable
    private fun getSizedTrades(account: Account): ImmutableList<SizedTrade> {
        return remember(account) {
            tradingProfiles.currentProfile.flatMapLatest { profile ->

                // Close all child windows
                orderFormWindowsManager.closeAll()

                val tradingRecord = tradingProfiles.getRecord(profile.id)

                tradingRecord.sizingTrades
                    .allTrades
                    .mapList { sizingTrade -> sizingTrade.size(account) }
                    .map { it.toImmutableList() }
            }
        }.collectAsState(persistentListOf()).value
    }

    private fun SizingTrade.size(account: Account): SizedTrade {

        val entryStopComparison = entry.compareTo(stop)

        val spread = (entry - stop).abs()

        val calculatedQuantity = when {
            spread.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.riskAmount / spread).setScale(0, RoundingMode.FLOOR)
        }

        val maxAffordableQuantity = when {
            entry.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.balancePerTrade * account.leverage) / entry
        }

        return SizedTrade(
            id = id,
            ticker = ticker,
            entry = entry.toPlainString(),
            stop = stop.toPlainString(),
            side = when {
                entryStopComparison > 0 -> TradeSide.Long.strValue
                entryStopComparison < 0 -> TradeSide.Short.strValue
                else -> ""
            }.uppercase(),
            spread = spread.toPlainString(),
            calculatedQuantity = calculatedQuantity.toPlainString(),
            maxAffordableQuantity = maxAffordableQuantity.toPlainString(),
            target = when {
                entry > stop -> entry + spread // Long
                else -> entry - spread // Short
            }.toPlainString(),
            color = when {
                entryStopComparison > 0 -> AppColor.ProfitGreen
                entryStopComparison < 0 -> AppColor.LossRed
                else -> Color.Transparent
            },
        )
    }
}

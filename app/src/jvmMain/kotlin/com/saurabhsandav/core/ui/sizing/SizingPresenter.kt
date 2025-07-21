package com.saurabhsandav.core.ui.sizing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.sizing.model.SizingEvent
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.AddTrade
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.OpenLiveTrade
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.RemoveTrade
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.UpdateTradeEntry
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.UpdateTradeStop
import com.saurabhsandav.core.ui.sizing.model.SizingState
import com.saurabhsandav.core.ui.sizing.model.SizingState.SizedTrade
import com.saurabhsandav.core.ui.sizing.model.SizingState.TradeExecutionFormParams
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormModel
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.market.india.FinvasiaBroker
import com.saurabhsandav.trading.record.SizingTrade
import com.saurabhsandav.trading.record.model.Account
import com.saurabhsandav.trading.record.model.SizingTradeId
import com.saurabhsandav.trading.record.model.TradeSide
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.uuid.Uuid

@AssistedInject
internal class SizingPresenter(
    @Assisted private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val account: Flow<Account>,
    private val tradingProfiles: TradingProfiles,
) {

    private val sizingtrades = coroutineScope.async { tradingProfiles.getRecord(profileId).sizingTrades }

    val executionFormWindowsManager = AppWindowsManager<TradeExecutionFormParams>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        val account by account.collectAsState(
            Account(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
            ),
        )

        return@launchMolecule SizingState(
            sizedTrades = getSizedTrades(account),
            executionFormWindowsManager = executionFormWindowsManager,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: SizingEvent) {

        when (event) {
            is AddTrade -> addTrade(event.symbolId)
            is UpdateTradeEntry -> updateTradeEntry(event.id, event.entry)
            is UpdateTradeStop -> updateTradeStop(event.id, event.stop)
            is OpenLiveTrade -> openLiveTrade(event.id)
            is RemoveTrade -> removeTrade(event.id)
        }
    }

    private fun addTrade(symbolId: SymbolId) = coroutineScope.launchUnit {

        sizingtrades.await().new(
            brokerId = FinvasiaBroker.Id,
            symbolId = symbolId,
            entry = 100.toBigDecimal(),
            stop = 90.toBigDecimal(),
        )
    }

    private fun updateTradeEntry(
        id: SizingTradeId,
        entry: String,
    ) = coroutineScope.launchUnit {

        val entryBD = entry.toBigDecimalOrNull() ?: return@launchUnit

        sizingtrades.await().updateEntry(
            id = id,
            entry = entryBD,
        )
    }

    private fun updateTradeStop(
        id: SizingTradeId,
        stop: String,
    ) = coroutineScope.launchUnit {

        val stopBD = stop.toBigDecimalOrNull() ?: return@launchUnit

        sizingtrades.await().updateStop(
            id = id,
            stop = stopBD,
        )
    }

    private fun removeTrade(id: SizingTradeId) = coroutineScope.launchUnit {

        sizingtrades.await().delete(id)
    }

    private fun openLiveTrade(id: SizingTradeId) = coroutineScope.launchUnit {

        val sizingTrade = sizingtrades.await().getById(id).first()

        val entryStopComparison = sizingTrade.entry.compareTo(sizingTrade.stop)

        val isBuy = when {
            // Short
            entryStopComparison < 0 -> false
            // Long (even if entry and stop are the same). Form should validate before saving.
            else -> true
        }

        val spread = (sizingTrade.entry - sizingTrade.stop).abs()

        val target = when {
            isBuy -> sizingTrade.entry + spread
            else -> sizingTrade.entry - spread
        }

        val account = account.first()

        val calculatedQuantity = when {
            spread.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.riskAmount / spread).setScale(0, RoundingMode.FLOOR)
        }

        val maxAffordableQuantity = when {
            sizingTrade.entry.compareTo(BigDecimal.ZERO) == 0 -> BigDecimal.ZERO
            else -> (account.balancePerTrade * account.leverage) / sizingTrade.entry
        }

        val params = TradeExecutionFormParams(
            id = Uuid.random(),
            profileId = profileId,
            formType = TradeExecutionFormType.NewSized(
                formModel = TradeExecutionFormModel(
                    instrument = Instrument.Equity,
                    symbolId = sizingTrade.symbolId,
                    quantity = calculatedQuantity.min(maxAffordableQuantity).toPlainString(),
                    isBuy = isBuy,
                    price = sizingTrade.entry.toPlainString(),
                ),
                stop = sizingTrade.stop,
                target = target,
            ),
        )

        executionFormWindowsManager.newWindow(params)
    }

    @Composable
    private fun getSizedTrades(account: Account): List<SizedTrade> {
        return remember(account) {
            flow {

                sizingtrades
                    .await()
                    .allTrades
                    .mapList { sizingTrade -> sizingTrade.size(account) }
                    .emitInto(this)
            }
        }.collectAsState(emptyList()).value
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
            ticker = symbolId.value,
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

    @AssistedFactory
    fun interface Factory {

        fun create(coroutineScope: CoroutineScope): SizingPresenter
    }
}

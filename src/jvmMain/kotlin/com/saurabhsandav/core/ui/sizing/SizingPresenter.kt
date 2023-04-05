package com.saurabhsandav.core.ui.sizing

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.SizingTrade
import com.saurabhsandav.core.trades.SizingTradesRepo
import com.saurabhsandav.core.trades.model.Account
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.sizing.model.SizedTrade
import com.saurabhsandav.core.ui.sizing.model.SizingEvent
import com.saurabhsandav.core.ui.sizing.model.SizingEvent.*
import com.saurabhsandav.core.ui.sizing.model.SizingState
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode

@Stable
internal class SizingPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val sizingTradesRepo: SizingTradesRepo = appModule.sizingTradesRepo,
) {

    private val events = MutableSharedFlow<SizingEvent>(extraBufferCapacity = Int.MAX_VALUE)

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
        )
    }

    fun event(event: SizingEvent) {
        events.tryEmit(event)
    }

    private fun addTrade(ticker: String) = coroutineScope.launchUnit {

        sizingTradesRepo.new(
            ticker = ticker,
            entry = 100.toBigDecimal(),
            stop = 90.toBigDecimal(),
        )
    }

    private fun updateTradeEntry(id: Long, entry: String) = coroutineScope.launchUnit {

        val entryBD = entry.toBigDecimalOrNull() ?: return@launchUnit

        sizingTradesRepo.updateEntry(
            id = id,
            entry = entryBD,
        )
    }

    private fun updateTradeStop(id: Long, stop: String) = coroutineScope.launchUnit {

        val stopBD = stop.toBigDecimalOrNull() ?: return@launchUnit

        sizingTradesRepo.updateStop(
            id = id,
            stop = stopBD,
        )
    }

    private fun openTrade(id: Long) {
    }

    private fun removeTrade(id: Long) = coroutineScope.launchUnit {
        sizingTradesRepo.delete(id)
    }

    @Composable
    private fun getSizedTrades(account: Account): ImmutableList<SizedTrade> {
        return remember(account) {
            sizingTradesRepo.allTrades
                .mapList { sizingTrade -> sizingTrade.size(account) }
                .map { it.toImmutableList() }
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

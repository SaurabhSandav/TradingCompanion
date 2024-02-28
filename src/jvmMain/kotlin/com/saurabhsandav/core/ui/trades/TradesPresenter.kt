package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.*
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeFilter
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeSort
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.trades.model.TradesEvent
import com.saurabhsandav.core.ui.trades.model.TradesEvent.*
import com.saurabhsandav.core.ui.trades.model.TradesState
import com.saurabhsandav.core.ui.trades.model.TradesState.*
import com.saurabhsandav.core.utils.format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Stable
internal class TradesPresenter(
    coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val isFocusModeEnabled = MutableStateFlow(true)
    private val tradeFilter = MutableStateFlow(TradeFilter())
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradesState(
            tradesList = getTradesList(),
            errors = errors,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradesEvent) {

        when (event) {
            is OpenDetails -> onOpenDetails(event.id)
            is OpenChart -> onOpenChart(event.id)
            is SetFocusModeEnabled -> onSetFocusModeEnabled(event.isEnabled)
            is ApplyFilter -> onApplyFilter(event.tradeFilter)
        }
    }

    @Composable
    private fun getTradesList(): TradesList {

        val initial = remember {
            TradesList.Focused(
                openTrades = emptyList(),
                todayTrades = emptyList(),
                todayStats = null,
                pastTrades = emptyList()
            )
        }

        return produceState<TradesList>(initial) {

            val tradesRepo = tradingProfiles.getRecord(profileId).trades

            fun List<Trade>.toTradeListEntries(): List<TradeEntry> {
                return map { it.toTradeListEntry() }
            }

            val defaultFilter = TradeFilter()

            isFocusModeEnabled.flatMapLatest { focusMode ->

                fun List<Trade>.subListOrEmpty(from: Int = 0, to: Int = size): List<Trade> {
                    val fromC = from.coerceAtLeast(0)
                    val toC = to.coerceAtMost(size)
                    return if (toC <= fromC) emptyList() else subList(fromC, toC)
                }

                if (focusMode) {

                    tradesRepo
                        .getFiltered(
                            filter = TradeFilter(),
                            sort = TradeSort.OpenDescEntryDesc,
                        )
                        .flatMapLatest { trades ->

                            val firstClosedTradeIndex = trades.indexOfFirst { it.isClosed }

                            val firstNotTodayTradeIndex = trades.subListOrEmpty(from = firstClosedTradeIndex).run {
                                val tz = TimeZone.currentSystemDefault()
                                val startOfToday = Clock.System.now().toLocalDateTime(tz).date.atStartOfDayIn(tz)
                                firstClosedTradeIndex + indexOfFirst { it.entryTimestamp < startOfToday }
                            }

                            val todayTrades = trades.subListOrEmpty(firstClosedTradeIndex, firstNotTodayTradeIndex)

                            todayTrades.generateStats(tradesRepo).map { todayStats ->

                                TradesList.Focused(
                                    openTrades = trades.subListOrEmpty(to = firstClosedTradeIndex).toTradeListEntries(),
                                    todayTrades = todayTrades.toTradeListEntries(),
                                    todayStats = todayStats,
                                    pastTrades = trades.subListOrEmpty(from = firstNotTodayTradeIndex)
                                        .toTradeListEntries(),
                                )
                            }
                        }
                } else {
                    tradeFilter.flatMapLatest { filter -> tradesRepo.getFiltered(filter = filter) }
                        .map { trades ->
                            TradesList.All(
                                trades = trades.toTradeListEntries(),
                                isFiltered = tradeFilter.value != defaultFilter,
                            )
                        }
                }

            }.collect { value = it }
        }.value
    }

    private fun List<Trade>.generateStats(tradesRepo: TradesRepo): Flow<Stats?> {

        val closedTrades = filter { it.isClosed }.ifEmpty { return flowOf(null) }
        val closedTradesIds = closedTrades.map { it.id }

        return tradesRepo.getPrimaryStops(closedTradesIds).map { tradeStops ->

            val stats = closedTrades.map { trade ->

                val stop = tradeStops.find { it.tradeId == trade.id }

                val brokerage = trade.brokerageAtExit()!!
                val rValue = stop?.let { trade.rValueAt(pnl = brokerage.pnl, stop = it) }

                brokerage to rValue
            }

            val pnl = stats.sumOf { (brokerage, _) -> brokerage.pnl }.stripTrailingZeros()
            val netPnl = stats.sumOf { (brokerage, _) -> brokerage.netPNL }.stripTrailingZeros()
            val rValue = when {
                stats.all { it.second == null } -> null
                else -> stats.mapNotNull { it.second }.sumOf { it }
            }
            val rValueStr = rValue?.let { " | ${it.toPlainString()}R" }.orEmpty()

            Stats(
                pnl = "${pnl.toPlainString()}$rValueStr",
                isProfitable = pnl > BigDecimal.ZERO,
                netPnl = netPnl.toPlainString(),
                isNetProfitable = netPnl > BigDecimal.ZERO,
            )
        }
    }

    private fun Trade.toTradeListEntry(): TradeEntry {

        val instrumentCapitalized = instrument.strValue
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        fun formatDuration(duration: Duration): String {

            val durationSeconds = duration.inWholeSeconds

            return "%02d:%02d:%02d".format(
                durationSeconds / 3600,
                (durationSeconds % 3600) / 60,
                durationSeconds % 60,
            )
        }

        val durationStr = when {
            isClosed -> flowOf(formatDuration(exitTimestamp!! - entryTimestamp))
            else -> flow {
                while (true) {
                    emit(formatDuration(Clock.System.now() - entryTimestamp))
                    delay(1.seconds)
                }
            }
        }

        return TradeEntry(
            id = id,
            broker = "$broker ($instrumentCapitalized)",
            ticker = ticker,
            side = side.toString().uppercase(),
            quantity = when {
                !isClosed -> "$closedQuantity / $quantity"
                else -> quantity.toPlainString()
            },
            entry = averageEntry.toPlainString(),
            exit = averageExit?.toPlainString() ?: "",
            entryTime = TradeDateTimeFormatter.format(
                ldt = entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault())
            ),
            duration = durationStr,
            pnl = pnl.toPlainString(),
            isProfitable = pnl > BigDecimal.ZERO,
            netPnl = netPnl.toPlainString(),
            isNetProfitable = netPnl > BigDecimal.ZERO,
            fees = fees.toPlainString(),
        )
    }

    private fun onOpenDetails(id: TradeId) {

        tradeContentLauncher.openTrade(ProfileTradeId(profileId = profileId, tradeId = id))
    }

    private fun onOpenChart(id: TradeId) {

        tradeContentLauncher.openTradeReview(ProfileTradeId(profileId = profileId, tradeId = id))
    }

    private fun onSetFocusModeEnabled(isEnabled: Boolean) {
        isFocusModeEnabled.value = isEnabled
        if (isEnabled) tradeFilter.value = TradeFilter()
    }

    private fun onApplyFilter(newTradeFilter: TradeFilter) {
        isFocusModeEnabled.value = false
        tradeFilter.value = newTradeFilter
    }
}

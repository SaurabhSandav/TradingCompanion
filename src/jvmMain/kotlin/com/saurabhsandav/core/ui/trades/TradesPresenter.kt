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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradesState(
            tradesList = getTradesList(),
            errors = remember(errors) { errors.toImmutableList() },
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradesEvent) {

        when (event) {
            is OpenDetails -> onOpenDetails(event.id)
            is OpenChart -> onOpenChart(event.id)
            is SetFocusModeEnabled -> onSetFocusModeEnabled(event.isEnabled)
        }
    }

    @Composable
    private fun getTradesList(): TradesList {

        val initial = remember {
            TradesList.Focused(
                openTrades = persistentListOf(),
                todayTrades = persistentListOf(),
                todayStats = null,
                pastTrades = persistentListOf()
            )
        }

        return produceState<TradesList>(initial) {

            val tradesRepo = tradingProfiles.getRecord(profileId).trades

            fun List<Trade>.toTradeListEntries(): ImmutableList<TradeEntry> {
                return map { it.toTradeListEntry() }.toImmutableList()
            }

            isFocusModeEnabled.flatMapLatest { focusMode ->

                fun List<Trade>.subListOrEmpty(from: Int = 0, to: Int = size): List<Trade> {
                    return if (to <= from) emptyList() else subList(from, to)
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
                    tradesRepo
                        .getFiltered(filter = TradeFilter())
                        .map { trades -> TradesList.All(trades = trades.toTradeListEntries()) }
                }

            }.collect { value = it }
        }.value
    }

    private fun List<Trade>.generateStats(tradesRepo: TradesRepo): Flow<Stats?> {

        val flows = filter { it.isClosed }.map { trade ->
            tradesRepo.getPrimaryStop(trade.id).map { stop -> trade to stop }
        }

        if (flows.isEmpty()) return flowOf(null)

        return combine(flows) { tradeAndStopList ->

            val stats = tradeAndStopList.filter { it.first.isClosed }.map { tradeAndStop ->

                val trade = tradeAndStop.first
                val stop = tradeAndStop.second

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
    }
}

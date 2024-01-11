package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.brokerageAtExit
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.trades.rValueAt
import com.saurabhsandav.core.ui.common.TradeDateTimeFormatter
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.trades.model.TradesEvent
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenDetails
import com.saurabhsandav.core.ui.trades.model.TradesState
import com.saurabhsandav.core.ui.trades.model.TradesState.Stats
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.format
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    private var todayStats by mutableStateOf<Stats?>(null)
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradesState(
            openTrades = getOpenTrades().value,
            todayTrades = getTodayTrades().value,
            todayStats = todayStats,
            pastTrades = getPastTrades().value,
            errors = remember(errors) { errors.toImmutableList() },
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradesEvent) {

        when (event) {
            is OpenDetails -> onOpenDetails(event.id)
            is OpenChart -> onOpenChart(event.id)
        }
    }

    init {

        coroutineScope.launch {

            val tradesRepo = tradingProfiles.getRecord(profileId).trades

            tradesRepo
                .getFiltered(filter = TradeFilter { isToday() })
                .flatMapLatest { trades ->

                    val flows = trades
                        .filter { it.isClosed }
                        .map { trade -> tradesRepo.getPrimaryStop(trade.id).map { stop -> trade to stop } }

                    combine(flows) { tradeAndStopList ->

                        val stats = tradeAndStopList.filter { it.first.isClosed }.map { tradeAndStop ->

                            val trade = tradeAndStop.first
                            val stop = tradeAndStop.second

                            val brokerage = trade.brokerageAtExit()!!
                            val rValue = stop?.let { trade.rValueAt(pnl = brokerage.pnl, stop = it) }

                            Triple(brokerage.pnl, brokerage.netPNL, rValue)
                        }

                        val pnl = stats.sumOf { it.first }.stripTrailingZeros()
                        val netPnl = stats.sumOf { it.second }.stripTrailingZeros()
                        val rValue = when {
                            stats.all { it.third == null } -> null
                            else -> stats.mapNotNull { it.third }.sumOf { it }
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
                .collect { stats -> todayStats = stats }
        }
    }

    @Composable
    private fun getOpenTrades(): State<ImmutableList<TradeEntry>> {
        return getTrades { isOpen() }
    }

    @Composable
    private fun getTodayTrades(): State<ImmutableList<TradeEntry>> {
        return getTrades {
            isClosed()
            isToday()
        }
    }

    @Composable
    private fun getPastTrades(): State<ImmutableList<TradeEntry>> {
        return getTrades {
            isClosed()
            isBeforeToday()
        }
    }

    @Composable
    private fun getTrades(filterTransform: TradeFilterScope.() -> Unit): State<ImmutableList<TradeEntry>> {
        return remember {
            flow {

                tradingProfiles
                    .getRecord(profileId)
                    .trades
                    .getFiltered(TradeFilter(filterTransform))
                    .map { trades ->
                        trades
                            .map { it.toTradeListEntry() }
                            .toImmutableList()
                    }
                    .emitInto(this)
            }
        }.collectAsState(persistentListOf())
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

    private fun TradeFilterScope.isToday() {

        val tz = TimeZone.currentSystemDefault()
        val startOfToday = Clock.System.now().toLocalDateTime(tz).date.atStartOfDayIn(tz)

        instantRange(from = startOfToday)
    }

    private fun TradeFilterScope.isBeforeToday() {

        val tz = TimeZone.currentSystemDefault()
        val startOfToday = Clock.System.now().toLocalDateTime(tz).date.atStartOfDayIn(tz)

        instantRange(to = startOfToday)
    }
}

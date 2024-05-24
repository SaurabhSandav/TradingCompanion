package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.*
import androidx.paging.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.*
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeFilter
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeSort
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.trades.model.TradesEvent
import com.saurabhsandav.core.ui.trades.model.TradesEvent.*
import com.saurabhsandav.core.ui.trades.model.TradesState
import com.saurabhsandav.core.ui.trades.model.TradesState.Stats
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry.Item
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.math.BigDecimal
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class TradesPresenter(
    coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private var isFocusModeEnabled by mutableStateOf(true)
    private var tradeFilter by mutableStateOf(TradeFilter())
    private val errors = mutableStateListOf<UIErrorMessage>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradesState(
            tradeEntries = getTradeEntries(),
            isFocusModeEnabled = isFocusModeEnabled,
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
    private fun getTradeEntries(): Flow<PagingData<TradeEntry>> = remember {
        flow {

            val tradesRepo = tradingProfiles.getRecord(profileId).trades
            val pagingConfig = PagingConfig(
                pageSize = 70,
                enablePlaceholders = false,
                maxSize = 300,
            )

            snapshotFlow { isFocusModeEnabled to tradeFilter }.flatMapLatest { (isFocusModeEnabled, tradeFilter) ->

                Pager(
                    config = pagingConfig,
                    pagingSourceFactory = {

                        tradesRepo.getFilteredPagingSource(
                            filter = tradeFilter,
                            sort = if (isFocusModeEnabled) TradeSort.OpenDescEntryDesc else TradeSort.EntryDesc,
                        )
                    },
                ).flow.map { pagingData ->

                    val tz = TimeZone.currentSystemDefault()
                    val nowDate = Clock.System.now().toLocalDateTime(tz).date
                    val startOfToday = nowDate.atStartOfDayIn(tz)

                    @Suppress("UNCHECKED_CAST")
                    pagingData.insertSeparators { before, after ->

                        when {

                            // If before is the last trade
                            after == null -> null

                            // If focus mode is not enabled
                            !isFocusModeEnabled -> when {
                                before == null -> {

                                    TradeEntry.Section(
                                        type = when (tradeFilter) {
                                            TradeFilter() -> TradeEntry.Section.Type.All
                                            else -> TradeEntry.Section.Type.Filtered
                                        },
                                        count = tradesRepo.getFilteredCount(tradeFilter)
                                    )
                                }

                                else -> null
                            }

                            // If first trade is open
                            before == null && !after.isClosed -> TradeEntry.Section(
                                type = TradeEntry.Section.Type.Open,
                                count = tradesRepo.getFilteredCount(TradeFilter(isClosed = false))
                            )

                            // If either after is first trade or before is open
                            // And after is from today
                            (before == null || !before.isClosed) && after.isClosed && after.entryTimestamp >= startOfToday -> {

                                val filter = TradeFilter(instantFrom = startOfToday)

                                TradeEntry.Section(
                                    type = TradeEntry.Section.Type.Today,
                                    count = tradesRepo.getFilteredCount(filter),
                                    stats = tradesRepo.getFiltered(filter)
                                        .flatMapLatest { it.generateStats(tradesRepo) }
                                )
                            }

                            // If either after is first execution or before is from today
                            // And after is from before today
                            (before == null || !before.isClosed || before.entryTimestamp >= startOfToday)
                                    && after.isClosed && after.entryTimestamp < startOfToday -> {

                                val filter = TradeFilter(instantTo = startOfToday)

                                TradeEntry.Section(
                                    type = TradeEntry.Section.Type.Past,
                                    count = tradesRepo.getFilteredCount(filter),
                                )
                            }

                            else -> null
                        }
                    }.map { tradeOrEntry ->
                        when (tradeOrEntry) {
                            is Trade -> tradeOrEntry.toTradeEntryItem()
                            else -> tradeOrEntry
                        }
                    } as PagingData<TradeEntry>
                }
            }.emitInto(this)
        }
    }

    private fun List<Trade>.generateStats(tradesRepo: TradesRepo): Flow<Stats> {

        val closedTrades = filter { it.isClosed }.ifEmpty { error("generateStats: No trades") }
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

    private fun Trade.toTradeEntryItem(): Item {

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

        val duration = when {
            isClosed -> Item.Duration.Closed(
                str = formatDuration(exitTimestamp!! - entryTimestamp)
            )

            else -> Item.Duration.Open(
                flow = flow {
                    while (true) {
                        emit(formatDuration(Clock.System.now() - entryTimestamp))
                        delay(1.seconds)
                    }
                }
            )
        }

        return Item(
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
            entryTime = entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault()).format(TradeDateTimeFormat),
            duration = duration,
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
        isFocusModeEnabled = isEnabled
        if (isEnabled) tradeFilter = TradeFilter()
    }

    private fun onApplyFilter(newTradeFilter: TradeFilter) {
        isFocusModeEnabled = false
        tradeFilter = newTradeFilter
    }
}

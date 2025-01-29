package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.*
import androidx.paging.*
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trades.*
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.trades.model.TradesEvent
import com.saurabhsandav.core.ui.trades.model.TradesEvent.*
import com.saurabhsandav.core.ui.trades.model.TradesState
import com.saurabhsandav.core.ui.trades.model.TradesState.Stats
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry.Item
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.math.BigDecimal
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class TradesPresenter(
    private val coroutineScope: CoroutineScope,
    private val profileId: ProfileId,
    private val tradeContentLauncher: TradeContentLauncher,
    private val tradingProfiles: TradingProfiles,
) {

    private val tradingRecord = coroutineScope.async { tradingProfiles.getRecord(profileId) }
    private var isFocusModeEnabled by mutableStateOf(true)
    private var tradeFilter by mutableStateOf(TradeFilter())
    private val selectionManager = SelectionManager<TradeId>()

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradesState(
            tradeEntries = getTradeEntries(),
            isFocusModeEnabled = isFocusModeEnabled,
            selectionManager = selectionManager,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: TradesEvent) {

        when (event) {
            is OpenDetails -> onOpenDetails(event.id)
            is OpenChart -> onOpenChart(event.ids)
            is SetFocusModeEnabled -> onSetFocusModeEnabled(event.isEnabled)
            is ApplyFilter -> onApplyFilter(event.tradeFilter)
            is NewExecution -> onNewExecution()
            is DeleteTrades -> onDeleteTrades(event.ids)
            is AddTag -> onAddTag(event.tradesIds, event.tagId)
        }
    }

    @Composable
    private fun getTradeEntries(): Flow<PagingData<TradeEntry>> = remember {
        flow {

            val trades = tradingRecord.await().trades
            val pagingConfig = PagingConfig(
                pageSize = 70,
                enablePlaceholders = false,
                maxSize = 300,
            )

            snapshotFlow { isFocusModeEnabled to tradeFilter }.flatMapLatest { (isFocusModeEnabled, tradeFilter) ->

                Pager(
                    config = pagingConfig,
                    pagingSourceFactory = {

                        trades.getFilteredPagingSource(
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
                                        count = trades.getFilteredCount(tradeFilter)
                                    )
                                }

                                else -> null
                            }

                            // If first trade is open
                            before == null && !after.isClosed -> TradeEntry.Section(
                                type = TradeEntry.Section.Type.Open,
                                count = trades.getFilteredCount(TradeFilter(isClosed = false))
                            )

                            // If either after is first trade or before is open
                            // And after is from today
                            (before == null || !before.isClosed) && after.isClosed && after.entryTimestamp >= startOfToday -> {

                                val filter = TradeFilter(instantFrom = startOfToday)

                                TradeEntry.Section(
                                    type = TradeEntry.Section.Type.Today,
                                    count = trades.getFilteredCount(filter),
                                    stats = trades.getFiltered(filter)
                                        .flatMapLatest { it.generateStats(tradingRecord.await()) }
                                )
                            }

                            // If either after is first execution or before is from today
                            // And after is from before today
                            (before == null || !before.isClosed || before.entryTimestamp >= startOfToday)
                                    && after.isClosed && after.entryTimestamp < startOfToday -> {

                                val filter = TradeFilter(instantTo = startOfToday)

                                TradeEntry.Section(
                                    type = TradeEntry.Section.Type.Past,
                                    count = trades.getFilteredCount(filter),
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

    private fun List<Trade>.generateStats(tradingRecord: TradingRecord): Flow<Stats> {

        val closedTrades = filter { it.isClosed }.ifEmpty { return emptyFlow() }
        val closedTradesIds = closedTrades.map { it.id }

        return tradingRecord.stops.getPrimary(closedTradesIds).map { tradeStops ->

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

    private fun onOpenChart(ids: List<TradeId>) {

        tradeContentLauncher.openTradeReview(
            tradeIds = ids.map { ProfileTradeId(profileId = profileId, tradeId = it) },
        )
    }

    private fun onSetFocusModeEnabled(isEnabled: Boolean) {
        isFocusModeEnabled = isEnabled
        if (isEnabled) tradeFilter = TradeFilter()
    }

    private fun onApplyFilter(newTradeFilter: TradeFilter) {
        isFocusModeEnabled = false
        tradeFilter = newTradeFilter
    }

    private fun onNewExecution() {
        tradeContentLauncher.openExecutionForm(profileId, TradeExecutionFormType.New)
    }

    private fun onDeleteTrades(ids: List<TradeId>) = coroutineScope.launchUnit {

        tradingRecord.await().trades.delete(ids)
    }

    private fun onAddTag(
        tradeIds: List<TradeId>,
        tagId: TradeTagId,
    ) = coroutineScope.launchUnit {

        tradingRecord.await().tags.add(tradeIds, tagId)
    }
}

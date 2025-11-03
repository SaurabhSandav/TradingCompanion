package com.saurabhsandav.core.ui.trades

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.common.getBrokerTitle
import com.saurabhsandav.core.ui.common.getSymbolTitle
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.ui.tradecontent.TradeContentLauncher
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.trades.model.TradesEvent
import com.saurabhsandav.core.ui.trades.model.TradesEvent.AddTag
import com.saurabhsandav.core.ui.trades.model.TradesEvent.ApplyFilter
import com.saurabhsandav.core.ui.trades.model.TradesEvent.DeleteTrades
import com.saurabhsandav.core.ui.trades.model.TradesEvent.NewExecution
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenChart
import com.saurabhsandav.core.ui.trades.model.TradesEvent.OpenDetails
import com.saurabhsandav.core.ui.trades.model.TradesEvent.SetFocusModeEnabled
import com.saurabhsandav.core.ui.trades.model.TradesState
import com.saurabhsandav.core.ui.trades.model.TradesState.Stats
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry.Item
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.sumOf
import com.saurabhsandav.trading.record.Trade
import com.saurabhsandav.trading.record.TradeDisplay
import com.saurabhsandav.trading.record.TradingRecord
import com.saurabhsandav.trading.record.brokerageAtExit
import com.saurabhsandav.trading.record.model.TradeFilter
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeSort
import com.saurabhsandav.trading.record.model.TradeTagId
import com.saurabhsandav.trading.record.rValueAt
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@AssistedInject
internal class TradesPresenter(
    @Assisted private val coroutineScope: CoroutineScope,
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

                        trades.getDisplayFilteredPagingSource(
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
                                        count = trades.getFilteredCount(tradeFilter),
                                    )
                                }

                                else -> null
                            }

                            // If first trade is open
                            before == null && !after.isClosed -> TradeEntry.Section(
                                type = TradeEntry.Section.Type.Open,
                                count = trades.getFilteredCount(TradeFilter(isClosed = false)),
                            )

                            // If either after is first trade or before is open
                            // And after is from today
                            (before == null || !before.isClosed) &&
                                after.isClosed &&
                                after.entryTimestamp >= startOfToday -> {

                                val filter = TradeFilter(instantFrom = startOfToday)

                                TradeEntry.Section(
                                    type = TradeEntry.Section.Type.Today,
                                    count = trades.getFilteredCount(filter),
                                    stats = trades.getFiltered(filter)
                                        .flatMapLatest { it.generateStats(tradingRecord.await()) },
                                )
                            }

                            // If either after is first execution or before is from today
                            // And after is from before today
                            (before == null || !before.isClosed || before.entryTimestamp >= startOfToday) &&
                                after.isClosed &&
                                after.entryTimestamp < startOfToday -> {

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
                            is TradeDisplay -> tradeOrEntry.toTradeEntryItem()
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

                val broker = tradingRecord.brokerProvider.getBroker(trade.brokerId)
                val brokerage = trade.brokerageAtExit(broker)!!
                val rValue = stop?.let { trade.rValueAt(pnl = brokerage.pnl, stop = it) }

                brokerage to rValue
            }

            val pnl = stats.sumOf { (brokerage, _) -> brokerage.pnl }
            val netPnl = stats.sumOf { (brokerage, _) -> brokerage.netPNL }
            val rValue = when {
                stats.all { it.second == null } -> null
                else -> stats.mapNotNull { it.second }.sumOf { it }
            }
            val rValueStr = rValue?.let { " | ${it}R" }.orEmpty()

            Stats(
                pnl = "$pnl$rValueStr",
                isProfitable = pnl > KBigDecimal.Zero,
                netPnl = netPnl.toString(),
                isNetProfitable = netPnl > KBigDecimal.Zero,
            )
        }
    }

    private fun TradeDisplay.toTradeEntryItem(): Item {

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
                str = formatDuration(exitTimestamp!! - entryTimestamp),
            )

            else -> Item.Duration.Open(
                flow = flow {
                    while (true) {
                        emit(formatDuration(Clock.System.now() - entryTimestamp))
                        delay(1.seconds)
                    }
                },
            )
        }

        return Item(
            id = id,
            broker = getBrokerTitle(),
            ticker = getSymbolTitle(),
            side = side.toString().uppercase(),
            quantity = when {
                !isClosed -> "$closedQuantity / $quantity"
                else -> quantity.toString()
            },
            entry = averageEntry.toString(),
            exit = averageExit?.toString() ?: "",
            entryTime = entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault()).format(TradeDateTimeFormat),
            duration = duration,
            pnl = pnl.toString(),
            isProfitable = pnl > KBigDecimal.Zero,
            netPnl = netPnl.toString(),
            isNetProfitable = netPnl > KBigDecimal.Zero,
            fees = fees.toString(),
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

    @AssistedFactory
    fun interface Factory {

        fun create(coroutineScope: CoroutineScope): TradesPresenter
    }
}

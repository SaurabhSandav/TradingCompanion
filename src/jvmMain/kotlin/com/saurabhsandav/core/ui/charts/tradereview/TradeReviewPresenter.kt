package com.saurabhsandav.core.ui.charts.tradereview

import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.Trade
import com.saurabhsandav.core.trades.TradingRecord
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.ui.charts.ChartMarkersProvider
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent.MarkTrade
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent.SelectTrade
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradeEntry
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradeListItem
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeOrderMarker
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@Stable
internal class TradeReviewPresenter(
    private val coroutineScope: CoroutineScope,
    private val appModule: AppModule,
    private val onOpenChart: (
        ticker: String,
        start: Instant,
        end: Instant?,
    ) -> Unit,
    setMarkersProvider: (ChartMarkersProvider) -> Unit,
    private val tradingRecord: TradingRecord = appModule.tradingRecord,
) {

    private val events = MutableSharedFlow<TradeReviewEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val markedTradeIds = MutableStateFlow<PersistentSet<Long>>(persistentSetOf())

    val state = coroutineScope.launchMolecule(RecompositionClock.ContextClock) {

        CollectEffect(events) { event ->

            when (event) {
                is MarkTrade -> onMarkTrade(event.id, event.isMarked)
                is SelectTrade -> onSelectTrade(event.id)
            }
        }

        return@launchMolecule TradeReviewState(
            tradesItems = getTradeListEntries().value,
        )
    }

    init {
        setMarkersProvider(::getMarkers)
    }

    fun event(event: TradeReviewEvent) {
        events.tryEmit(event)
    }

    @Composable
    private fun getTradeListEntries(): State<ImmutableList<TradeListItem>> {
        return remember {
            tradingRecord.trades.allTrades.combine(markedTradeIds) { trades, markedTradeIds ->
                trades
                    .groupBy { it.entryTimestamp.date }
                    .map { (date, list) ->
                        listOf(
                            date.toTradeListDayHeader(),
                            TradeListItem.Entries(
                                list.map { it.toTradeListEntry(it.id in markedTradeIds) }
                                    .toImmutableList()
                            ),
                        )
                    }.flatten().toImmutableList()
            }
        }.collectAsState(persistentListOf())
    }

    private fun LocalDate.toTradeListDayHeader(): TradeListItem.DayHeader {
        val formatted = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(toJavaLocalDate())
        return TradeListItem.DayHeader(formatted)
    }

    private fun Trade.toTradeListEntry(isMarked: Boolean): TradeEntry {

        val instrumentCapitalized = instrument
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val timeZone = TimeZone.of("Asia/Kolkata")
        val entryInstant = entryTimestamp.toInstant(timeZone)
        val exitInstant = exitTimestamp?.toInstant(timeZone)
        val s = exitInstant?.let { (it - entryInstant).inWholeSeconds }

        val duration = s?.let { "%02d:%02d:%02d".format(it / 3600, (it % 3600) / 60, (it % 60)) }

        return TradeEntry(
            isMarked = isMarked,
            id = id,
            broker = "$broker ($instrumentCapitalized)",
            ticker = ticker,
            side = side.toString().uppercase(),
            quantity = (lots?.let { "$closedQuantity / $quantity ($it ${if (it == 1) "lot" else "lots"})" }
                ?: "$closedQuantity / $quantity").toString(),
            entry = averageEntry.toPlainString(),
            exit = averageExit?.toPlainString() ?: "",
            duration = "${entryTimestamp.time} -> ${exitTimestamp?.time ?: "Now"}\n${duration?.let { "($it)" }}",
            pnl = pnl.toPlainString(),
            isProfitable = pnl > BigDecimal.ZERO,
            netPnl = netPnl.toPlainString(),
            isNetProfitable = netPnl > BigDecimal.ZERO,
        )
    }

    private fun onMarkTrade(id: Long, isMarked: Boolean) {
        markedTradeIds.value = if (isMarked) markedTradeIds.value.add(id) else markedTradeIds.value.remove(id)
    }

    private fun onSelectTrade(id: Long) = coroutineScope.launchUnit {

        // Mark selected trade
        markedTradeIds.value = markedTradeIds.value.add(id)

        val trade = tradingRecord.trades.getById(id).first()
        val start = trade.entryTimestamp.toInstant(TimeZone.currentSystemDefault())
        val end = trade.exitTimestamp?.toInstant(TimeZone.currentSystemDefault())

        // Show trade on chart
        onOpenChart(trade.ticker, start, end)
    }

    private fun getMarkers(ticker: String, candleSeries: CandleSeries): Flow<List<SeriesMarker>> {

        fun Instant.markerTime(): Instant {
            val markerCandleIndex = candleSeries.indexOfLast { it.openInstant <= this }
            return candleSeries[markerCandleIndex].openInstant
        }

        val candlesInstantRange = candleSeries.instantRange.value ?: return emptyFlow()
        val ldtRange = candlesInstantRange.start.toLocalDateTime(TimeZone.currentSystemDefault())..
                candlesInstantRange.endInclusive.toLocalDateTime(TimeZone.currentSystemDefault())

        val orderMarkers = markedTradeIds
            .flatMapLatest {
                tradingRecord.orders.getOrdersByTickerAndTradeIdsInInterval(ticker, it.toList(), ldtRange)
            }
            .mapList { order ->

                val orderInstant = order.timestamp.toInstant(TimeZone.currentSystemDefault())

                TradeOrderMarker(
                    instant = orderInstant.markerTime(),
                    orderType = order.type,
                    price = order.price,
                )
            }

        val tradeMarkers = markedTradeIds
            .flatMapLatest { tradingRecord.trades.getByTickerAndIdsInInterval(ticker, it.toList(), ldtRange) }
            .map { trades ->
                trades.flatMap { trade ->

                    val entryInstant = trade.entryTimestamp.toInstant(TimeZone.currentSystemDefault())

                    buildList {

                        add(
                            TradeMarker(
                                instant = entryInstant.markerTime(),
                                isEntry = true,
                            )
                        )

                        if (trade.isClosed) {

                            val exitInstant = trade.exitTimestamp!!.toInstant(TimeZone.currentSystemDefault())

                            add(
                                TradeMarker(
                                    instant = exitInstant.markerTime(),
                                    isEntry = false,
                                )
                            )
                        }
                    }
                }
            }

        return orderMarkers.combine(tradeMarkers) { orderMkrs, tradeMkrs -> orderMkrs + tradeMkrs }
            .flowOn(Dispatchers.IO)
    }
}

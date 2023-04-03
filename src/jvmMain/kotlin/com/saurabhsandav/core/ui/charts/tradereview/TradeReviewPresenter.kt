package com.saurabhsandav.core.ui.charts.tradereview

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.Trade
import com.saurabhsandav.core.chart.data.SeriesMarker
import com.saurabhsandav.core.chart.data.SeriesMarkerPosition
import com.saurabhsandav.core.chart.data.SeriesMarkerShape
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.trades.TradeOrdersRepo
import com.saurabhsandav.core.trades.TradesRepo
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.instantRange
import com.saurabhsandav.core.ui.charts.ChartMarkersProvider
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent.MarkTrade
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewEvent.SelectTrade
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradeEntry
import com.saurabhsandav.core.ui.charts.tradereview.model.TradeReviewState.TradeListItem
import com.saurabhsandav.core.ui.common.CollectEffect
import com.saurabhsandav.core.ui.common.chart.offsetTimeForChart
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
    private val tradesRepo: TradesRepo = appModule.tradesRepo,
    private val tradeOrdersRepo: TradeOrdersRepo = appModule.tradeOrdersRepo,
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
            tradesRepo.allTrades.combine(markedTradeIds) { trades, markedTradeIds ->
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

        val trade = tradesRepo.getById(id).first()
        val start = trade.entryTimestamp.toInstant(TimeZone.currentSystemDefault())
        val end = trade.exitTimestamp?.toInstant(TimeZone.currentSystemDefault())

        // Show trade on chart
        onOpenChart(trade.ticker, start, end)
    }

    private fun getMarkers(ticker: String, candleSeries: CandleSeries): Flow<List<SeriesMarker>> {

        fun Instant.markerTime(): Time {

            val markerCandleIndex = candleSeries.indexOfLast { it.openInstant <= this }
            val candleOpenInstant = candleSeries[markerCandleIndex].openInstant
            val offsetTime = candleOpenInstant.offsetTimeForChart()

            return Time.UTCTimestamp(offsetTime)
        }

        val candlesInstantRange = candleSeries.instantRange ?: return emptyFlow()
        val ldtRange = candlesInstantRange.start.toLocalDateTime(TimeZone.currentSystemDefault())..
                candlesInstantRange.endInclusive.toLocalDateTime(TimeZone.currentSystemDefault())

        val orderMarkers = markedTradeIds
            .flatMapLatest { tradeOrdersRepo.getOrdersByTickerAndTradeIdsInInterval(ticker, it.toList(), ldtRange) }
            .mapList { order ->

                val orderInstant = order.timestamp.toInstant(TimeZone.currentSystemDefault())

                SeriesMarker(
                    time = orderInstant.markerTime(),
                    position = when (order.type) {
                        OrderType.Buy -> SeriesMarkerPosition.BelowBar
                        OrderType.Sell -> SeriesMarkerPosition.AboveBar
                    },
                    shape = when (order.type) {
                        OrderType.Buy -> SeriesMarkerShape.ArrowUp
                        OrderType.Sell -> SeriesMarkerShape.ArrowDown
                    },
                    color = when (order.type) {
                        OrderType.Buy -> Color.Green
                        OrderType.Sell -> Color.Red
                    },
                    text = when (order.type) {
                        OrderType.Buy -> order.price.toPlainString()
                        OrderType.Sell -> order.price.toPlainString()
                    },
                )
            }.flowOn(Dispatchers.IO)

        val tradeMarkers = markedTradeIds
            .flatMapLatest { tradesRepo.getByTickerAndIdsInInterval(ticker, it.toList(), ldtRange) }
            .map { trades ->
                trades.flatMap { trade ->

                    val entryInstant = trade.entryTimestamp.toInstant(TimeZone.currentSystemDefault())

                    buildList {

                        add(
                            SeriesMarker(
                                time = entryInstant.markerTime(),
                                position = SeriesMarkerPosition.AboveBar,
                                shape = SeriesMarkerShape.Circle,
                                color = Color.Green,
                            )
                        )

                        if (trade.isClosed) {

                            val exitInstant = trade.exitTimestamp!!.toInstant(TimeZone.currentSystemDefault())

                            add(
                                SeriesMarker(
                                    time = exitInstant.markerTime(),
                                    position = SeriesMarkerPosition.AboveBar,
                                    shape = SeriesMarkerShape.Circle,
                                    color = Color.Red,
                                )
                            )
                        }
                    }
                }
            }.flowOn(Dispatchers.IO)

        return orderMarkers.combine(tradeMarkers) { orderMkrs, tradeMkrs -> orderMkrs + tradeMkrs }
    }
}

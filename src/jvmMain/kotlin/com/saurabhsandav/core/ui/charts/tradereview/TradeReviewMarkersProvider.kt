package com.saurabhsandav.core.ui.charts.tradereview

import androidx.compose.runtime.Stable
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.ui.charts.ChartMarkersProvider
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Stable
internal class TradeReviewMarkersProvider(
    private val appModule: AppModule,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
    private val appPrefs: FlowSettings = appModule.appPrefs,
) : ChartMarkersProvider {

    private val markedTradeIds = MutableStateFlow<Set<Long>>(persistentSetOf())

    override fun provideMarkers(ticker: String, candleSeries: CandleSeries): Flow<List<SeriesMarker>> {

        fun Instant.markerTime(): Instant {
            val markerCandleIndex = candleSeries.indexOfLast { it.openInstant <= this }
            return candleSeries[markerCandleIndex].openInstant
        }

        val candlesInstantRange = candleSeries.instantRange.value ?: return emptyFlow()
        val ldtRange = candlesInstantRange.start.toLocalDateTime(TimeZone.currentSystemDefault())..
                candlesInstantRange.endInclusive.toLocalDateTime(TimeZone.currentSystemDefault())

        val reviewProfile = appPrefs.getLongOrNullFlow(PrefKeys.TradeReviewTradingProfile)
            .flatMapLatest { id -> if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null) }
            .filterNotNull()

        val executionMarkers = reviewProfile
            .flatMapLatest { profile ->
                markedTradeIds.flatMapLatest { tradeIds ->

                    val tradingRecord = tradingProfiles.getRecord(profile.id)

                    tradingRecord.executions.getExecutionsByTickerAndTradeIdsInInterval(
                        ticker = ticker,
                        ids = tradeIds.toList(),
                        range = ldtRange,
                    )
                }
            }
            .mapList { execution ->

                val executionInstant = execution.timestamp.toInstant(TimeZone.currentSystemDefault())

                TradeExecutionMarker(
                    instant = executionInstant.markerTime(),
                    side = execution.side,
                    price = execution.price,
                )
            }

        val tradeMarkers = reviewProfile
            .flatMapLatest { profile ->
                markedTradeIds.flatMapLatest { tradeIds ->

                    val tradingRecord = tradingProfiles.getRecord(profile.id)

                    tradingRecord.trades.getByTickerAndIdsInInterval(ticker, tradeIds.toList(), ldtRange)
                }
            }
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

        return executionMarkers.combine(tradeMarkers) { executionMkrs, tradeMkrs -> executionMkrs + tradeMkrs }
            .flowOn(Dispatchers.IO)
    }

    fun setMarkedTradeIds(ids: Set<Long>) {
        markedTradeIds.value = ids
    }
}

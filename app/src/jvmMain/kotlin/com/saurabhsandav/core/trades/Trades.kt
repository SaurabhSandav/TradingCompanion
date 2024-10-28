package com.saurabhsandav.core.trades

import androidx.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.core.thirdparty.sqldelight_paging.QueryPagingSource
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.trades.model.TradeFilter
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeSort
import com.saurabhsandav.core.utils.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import kotlinx.datetime.format.char

class Trades internal constructor(
    private val appDispatchers: AppDispatchers,
    private val tradesDB: TradesDB,
    private val executions: Executions,
) {

    suspend fun delete(ids: List<TradeId>) = executions.deleteTrades(ids)

    val allTrades: Flow<List<Trade>>
        get() = tradesDB.tradeQueries.getAll().asFlow().mapToList(appDispatchers.IO)

    fun exists(id: Long): Flow<Boolean> {
        return tradesDB.tradeQueries.exists(TradeId(id)).asFlow().mapToOne(appDispatchers.IO)
    }

    fun exists(ids: List<Long>): Flow<Map<Long, Boolean>> {
        return tradesDB.tradeQueries
            .getExistingIds(ids.map(::TradeId))
            .asFlow()
            .mapToList(appDispatchers.IO)
            .map { existingIds -> ids.associateWith { id -> TradeId(id) in existingIds } }
    }

    fun getById(id: TradeId): Flow<Trade> {
        return tradesDB.tradeQueries.getById(id).asFlow().mapToOne(appDispatchers.IO)
    }

    fun getByIds(ids: List<TradeId>): Flow<List<Trade>> {
        return tradesDB.tradeQueries.getByIds(ids).asFlow().mapToList(appDispatchers.IO)
    }

    fun getFilteredCount(filter: TradeFilter): Flow<Long> {

        fun Boolean.toLong() = if (this) 1L else 0L

        val query = tradesDB.tradeQueries.getFilteredCount(
            isClosed = filter.isClosed,
            side = filter.side,
            from = filter.instantFrom?.toString(),
            to = filter.instantTo?.toString(),
            timeFrom = filter.timeFrom?.toOffsetFilterTimeString(),
            timeTo = filter.timeTo?.toOffsetFilterTimeString(),
            pnlFrom = filter.pnlFrom?.toDouble(),
            pnlTo = filter.pnlTo?.toDouble(),
            filterByNetPnl = filter.filterByNetPnl.toLong(),
            hasNotes = filter.hasNotes?.toLong(),
            tags = filter.tags,
            tagsCount = when {
                filter.tags.isEmpty() -> null
                filter.matchAllTags -> filter.tags.size.toLong()
                else -> -1
            },
            tickers = filter.tickers,
            tickersCount = filter.tickers.size.toLong(),
        )

        return query.asFlow().mapToOne(appDispatchers.IO)
    }

    fun getFiltered(
        filter: TradeFilter,
        sort: TradeSort = TradeSort.EntryDesc,
    ): Flow<List<Trade>> {

        fun Boolean.toLong() = if (this) 1L else 0L

        val query = tradesDB.tradeQueries.getFiltered(
            isClosed = filter.isClosed,
            side = filter.side,
            from = filter.instantFrom?.toString(),
            to = filter.instantTo?.toString(),
            timeFrom = filter.timeFrom?.toOffsetFilterTimeString(),
            timeTo = filter.timeTo?.toOffsetFilterTimeString(),
            pnlFrom = filter.pnlFrom?.toDouble(),
            pnlTo = filter.pnlTo?.toDouble(),
            filterByNetPnl = filter.filterByNetPnl.toLong(),
            hasNotes = filter.hasNotes?.toLong(),
            tags = filter.tags,
            tagsCount = when {
                filter.tags.isEmpty() -> null
                filter.matchAllTags -> filter.tags.size.toLong()
                else -> -1
            },
            tickers = filter.tickers,
            tickersCount = filter.tickers.size.toLong(),
            sortOpenFirst = (sort == TradeSort.OpenDescEntryDesc).toLong(),
        )

        return query.asFlow().mapToList(appDispatchers.IO)
    }

    fun getFilteredPagingSource(
        filter: TradeFilter,
        sort: TradeSort = TradeSort.EntryDesc,
    ): PagingSource<Int, Trade> {

        fun Boolean.toLong() = if (this) 1L else 0L

        return QueryPagingSource(
            countQuery = tradesDB.tradeQueries.getFilteredCount(
                isClosed = filter.isClosed,
                side = filter.side,
                from = filter.instantFrom?.toString(),
                to = filter.instantTo?.toString(),
                timeFrom = filter.timeFrom?.toOffsetFilterTimeString(),
                timeTo = filter.timeTo?.toOffsetFilterTimeString(),
                pnlFrom = filter.pnlFrom?.toDouble(),
                pnlTo = filter.pnlTo?.toDouble(),
                filterByNetPnl = filter.filterByNetPnl.toLong(),
                hasNotes = filter.hasNotes?.toLong(),
                tags = filter.tags,
                tagsCount = when {
                    filter.tags.isEmpty() -> null
                    filter.matchAllTags -> filter.tags.size.toLong()
                    else -> -1
                },
                tickers = filter.tickers,
                tickersCount = filter.tickers.size.toLong(),
            ),
            transacter = tradesDB.tradeQueries,
            context = appDispatchers.IO,
            queryProvider = { limit, offset ->

                tradesDB.tradeQueries.getFilteredPaged(
                    isClosed = filter.isClosed,
                    side = filter.side,
                    from = filter.instantFrom?.toString(),
                    to = filter.instantTo?.toString(),
                    timeFrom = filter.timeFrom?.toOffsetFilterTimeString(),
                    timeTo = filter.timeTo?.toOffsetFilterTimeString(),
                    pnlFrom = filter.pnlFrom?.toDouble(),
                    pnlTo = filter.pnlTo?.toDouble(),
                    filterByNetPnl = filter.filterByNetPnl.toLong(),
                    hasNotes = filter.hasNotes?.toLong(),
                    tags = filter.tags,
                    tagsCount = when {
                        filter.tags.isEmpty() -> null
                        filter.matchAllTags -> filter.tags.size.toLong()
                        else -> -1
                    },
                    tickers = filter.tickers,
                    tickersCount = filter.tickers.size.toLong(),
                    sortOpenFirst = (sort == TradeSort.OpenDescEntryDesc).toLong(),
                    limit = limit,
                    offset = offset,
                )
            },
        )
    }

    fun getByTickerInInterval(
        ticker: String,
        range: ClosedRange<Instant>,
    ): Flow<List<Trade>> {
        return tradesDB.tradeQueries
            .getByTickerInInterval(
                ticker = ticker,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(appDispatchers.IO)
    }

    fun getByTickerAndIdsInInterval(
        ticker: String,
        ids: List<TradeId>,
        range: ClosedRange<Instant>,
    ): Flow<List<Trade>> {
        return tradesDB.tradeQueries
            .getByTickerAndIdsInInterval(
                ticker = ticker,
                ids = ids,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(appDispatchers.IO)
    }

    fun getSuggestedTickers(
        query: String,
        ignore: List<String>,
    ): Flow<List<String>> {
        return tradesDB.tradeQueries.getSuggestedTickers(ignore, query).asFlow().mapToList(appDispatchers.IO)
    }

    fun getForExecution(executionId: TradeExecutionId): Flow<List<Trade>> {
        return tradesDB.tradeToExecutionMapQueries
            .getTradesByExecution(executionId)
            .asFlow()
            .mapToList(appDispatchers.IO)
    }

    fun getWithoutExcursionsBefore(instant: Instant): Flow<List<Trade>> {
        return tradesDB.tradeQueries
            .getWithoutExcursionsBeforeTimestamp(instant)
            .asFlow()
            .mapToList(appDispatchers.IO)
    }

    private fun LocalTime.toOffsetFilterTimeString(): String {
        val utcOffset = TimeZone.currentSystemDefault().offsetAt(Clock.System.now())
        val offsetTime = LocalTime.fromSecondOfDay(toSecondOfDay() - utcOffset.totalSeconds)
        return offsetTime.format(FilterTimeFormat)
    }

    private companion object {

        val FilterTimeFormat = LocalTime.Format {
            hour()
            char(':')
            minute()
            char(':')
            second()
        }
    }
}

package com.saurabhsandav.core.trading.record

import androidx.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.thirdparty.sqldelight.paging.QueryPagingSource
import com.saurabhsandav.core.trading.record.model.TradeExecutionId
import com.saurabhsandav.core.trading.record.model.TradeFilter
import com.saurabhsandav.core.trading.record.model.TradeId
import com.saurabhsandav.core.trading.record.model.TradeSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetAt
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Instant

class Trades internal constructor(
    private val coroutineContext: CoroutineContext,
    private val tradesDB: TradesDB,
    private val executions: Executions,
) {

    suspend fun delete(ids: List<TradeId>) = executions.deleteTrades(ids)

    val allTrades: Flow<List<Trade>>
        get() = tradesDB.tradeQueries.getAll().asFlow().mapToList(coroutineContext)

    fun exists(id: Long): Flow<Boolean> {
        return tradesDB.tradeQueries.exists(TradeId(id)).asFlow().mapToOne(coroutineContext)
    }

    fun exists(ids: List<Long>): Flow<Map<Long, Boolean>> {
        return tradesDB.tradeQueries
            .getExistingIds(ids.map(::TradeId))
            .asFlow()
            .mapToList(coroutineContext)
            .map { existingIds -> ids.associateWith { id -> TradeId(id) in existingIds } }
    }

    fun getById(id: TradeId): Flow<Trade> {
        return tradesDB.tradeQueries.getById(id).asFlow().mapToOne(coroutineContext)
    }

    fun getByIdOrNull(id: TradeId): Flow<Trade?> {
        return tradesDB.tradeQueries.getById(id).asFlow().mapToOneOrNull(coroutineContext)
    }

    fun getByIds(ids: List<TradeId>): Flow<List<Trade>> {
        return tradesDB.tradeQueries.getByIds(ids).asFlow().mapToList(coroutineContext)
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

        return query.asFlow().mapToOne(coroutineContext)
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

        return query.asFlow().mapToList(coroutineContext)
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
            context = coroutineContext,
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
            .mapToList(coroutineContext)
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
            .mapToList(coroutineContext)
    }

    fun getSuggestedTickers(
        query: String,
        ignore: List<String>,
    ): Flow<List<String>> {
        return tradesDB.tradeQueries.getSuggestedTickers(ignore, query).asFlow().mapToList(coroutineContext)
    }

    fun getForExecution(executionId: TradeExecutionId): Flow<List<Trade>> {
        return tradesDB.tradeToExecutionMapQueries
            .getTradesByExecution(executionId)
            .asFlow()
            .mapToList(coroutineContext)
    }

    fun getWithoutExcursionsBefore(instant: Instant): Flow<List<Trade>> {
        return tradesDB.tradeQueries
            .getWithoutExcursionsBeforeTimestamp(instant)
            .asFlow()
            .mapToList(coroutineContext)
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

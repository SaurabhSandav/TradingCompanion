package com.saurabhsandav.trading.record

import androidx.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.kbigdecimal.toDouble
import com.saurabhsandav.paging.pagingsource.QueryPagingSource
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.model.TradeFilter
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeSort
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

    val allTradesDisplay: Flow<List<TradeDisplay>>
        get() = tradesDB.tradeDisplayQueries.getAll().asFlow().mapToList(coroutineContext)

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

    fun getDisplayByIdOrNull(id: TradeId): Flow<TradeDisplay?> {
        return tradesDB.tradeDisplayQueries.getById(id).asFlow().mapToOneOrNull(coroutineContext)
    }

    fun getDisplayByIds(ids: List<TradeId>): Flow<List<TradeDisplay>> {
        return tradesDB.tradeDisplayQueries.getByIds(ids).asFlow().mapToList(coroutineContext)
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
            symbolIds = filter.symbols,
            symbolsCount = filter.symbols.size.toLong(),
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
            symbolIds = filter.symbols,
            symbolsCount = filter.symbols.size.toLong(),
            sortOpenFirst = (sort == TradeSort.OpenDescEntryDesc).toLong(),
        )

        return query.asFlow().mapToList(coroutineContext)
    }

    fun getDisplayFilteredPagingSource(
        filter: TradeFilter,
        sort: TradeSort = TradeSort.EntryDesc,
    ): PagingSource<Int, TradeDisplay> {

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
                symbolIds = filter.symbols,
                symbolsCount = filter.symbols.size.toLong(),
            ),
            transacter = tradesDB.tradeQueries,
            context = coroutineContext,
            queryProvider = { limit, offset ->

                tradesDB.tradeDisplayQueries.getFilteredPaged(
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
                    symbolIds = filter.symbols,
                    symbolsCount = filter.symbols.size.toLong(),
                    sortOpenFirst = (sort == TradeSort.OpenDescEntryDesc).toLong(),
                    limit = limit,
                    offset = offset,
                )
            },
        )
    }

    fun getBySymbolInInterval(
        symbolId: SymbolId,
        range: ClosedRange<Instant>,
    ): Flow<List<Trade>> {
        return tradesDB.tradeQueries
            .getBySymbolInInterval(
                symbolId = symbolId,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(coroutineContext)
    }

    fun getBySymbolAndIdsInInterval(
        symbolId: SymbolId,
        ids: List<TradeId>,
        range: ClosedRange<Instant>,
    ): Flow<List<Trade>> {
        return tradesDB.tradeQueries
            .getBySymbolAndIdsInInterval(
                symbolId = symbolId,
                ids = ids,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(coroutineContext)
    }

    fun getSuggestedSymbols(
        query: String,
        ignore: List<SymbolId>,
    ): Flow<List<SymbolId>> {
        return tradesDB.tradeQueries.getSuggestedSymbols(ignore, query).asFlow().mapToList(coroutineContext)
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

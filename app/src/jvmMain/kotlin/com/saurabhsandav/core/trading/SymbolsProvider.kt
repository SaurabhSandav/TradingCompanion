package com.saurabhsandav.core.trading

import androidx.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.CachedSymbol
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.paging.pagingsource.QueryPagingSource
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import com.saurabhsandav.trading.record.Symbol as RecordSymbol

interface SymbolsProvider {

    suspend fun downloadAllLatestSymbols()

    fun getSymbolsFiltered(
        filterQuery: String? = null,
        instruments: List<Instrument> = emptyList(),
        exchange: String? = null,
    ): PagingSource<Int, CachedSymbol>

    fun getSymbol(
        brokerId: BrokerId,
        symbolId: SymbolId,
    ): Flow<CachedSymbol?>
}

fun SymbolsProvider(
    appDB: AppDB,
    appDispatchers: AppDispatchers,
    brokerProvider: AppBrokerProvider,
): SymbolsProvider = AppSymbolsProvider(
    appDB = appDB,
    appDispatchers = appDispatchers,
    brokerProvider = brokerProvider,
)

private class AppSymbolsProvider(
    private val appDB: AppDB,
    private val appDispatchers: AppDispatchers,
    private val brokerProvider: AppBrokerProvider,
) : SymbolsProvider {

    override suspend fun downloadAllLatestSymbols() = withContext(appDispatchers.IO) {

        brokerProvider.getAllIds().forEach { brokerId ->

            val broker = brokerProvider.getBroker(brokerId)
            val lastDownloadInstant = appDB.symbolDownloadTimestampQueries.get(brokerId).executeAsOneOrNull()
            val newSymbols = broker.downloadSymbols(lastDownloadInstant) ?: return@forEach

            appDB.transaction {

                // Clear previous symbols
                appDB.cachedSymbolQueries.clearByBroker(brokerId)

                // Insert new symbols
                for (symbol in newSymbols) {

                    appDB.cachedSymbolQueries.insert(
                        id = symbol.id,
                        brokerId = brokerId,
                        instrument = symbol.instrument,
                        exchange = symbol.exchange,
                        ticker = symbol.ticker,
                        description = symbol.description,
                        tickSize = symbol.tickSize,
                        quantityMultiplier = symbol.quantityMultiplier,
                    )
                }

                // Save download timestamp
                appDB.symbolDownloadTimestampQueries.put(brokerId, Clock.System.now())
            }
        }
    }

    override fun getSymbolsFiltered(
        filterQuery: String?,
        instruments: List<Instrument>,
        exchange: String?,
    ): PagingSource<Int, CachedSymbol> = QueryPagingSource(
        countQuery = appDB.cachedSymbolQueries.getFilteredCount(
            filterQuery = filterQuery,
            instrumentsCount = instruments.size.toLong(),
            instruments = instruments,
            exchange = exchange,
        ),
        transacter = appDB.cachedSymbolQueries,
        context = appDispatchers.IO,
        queryProvider = { limit, offset ->

            appDB.cachedSymbolQueries.getFiltered(
                filterQuery = filterQuery,
                instrumentsCount = instruments.size.toLong(),
                instruments = instruments,
                exchange = exchange,
                limit = limit,
                offset = offset,
            )
        },
    )

    override fun getSymbol(
        brokerId: BrokerId,
        symbolId: SymbolId,
    ): Flow<CachedSymbol?> {
        return appDB.cachedSymbolQueries
            .get(symbolId, brokerId)
            .asFlow()
            .onStart { downloadAllLatestSymbols() }
            .mapToOneOrNull(appDispatchers.IO)
    }
}

fun CachedSymbol.toRecordSymbol(): RecordSymbol = RecordSymbol(
    id = id,
    brokerId = brokerId,
    instrument = instrument,
    exchange = exchange,
    ticker = ticker,
    description = description,
)

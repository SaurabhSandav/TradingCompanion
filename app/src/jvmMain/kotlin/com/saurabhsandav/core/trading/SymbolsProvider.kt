package com.saurabhsandav.core.trading

import androidx.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.CachedSymbol
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.paging.pagingsource.QueryPagingSource
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import com.saurabhsandav.trading.record.Symbol as RecordSymbol

interface SymbolsProvider {

    suspend fun downloadAllLatestSymbols()

    suspend fun getSymbolsFilteredPagingSourceFactory(
        filterQuery: String? = null,
        instruments: List<Instrument> = emptyList(),
        exchange: String? = null,
    ): () -> PagingSource<Int, CachedSymbol>

    fun getSymbol(
        brokerId: BrokerId,
        symbolId: SymbolId,
    ): Flow<CachedSymbol?>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class AppSymbolsProvider(
    private val appDB: AppDB,
    private val appDispatchers: AppDispatchers,
    private val brokerProvider: BrokerProvider,
) : SymbolsProvider {

    private val downloadMutex = Mutex()

    override suspend fun downloadAllLatestSymbols() = downloadMutex.withLock {
        withContext(appDispatchers.IO) {

            brokerProvider.getAllIds().forEach { brokerId ->

                val broker = brokerProvider.getBroker(brokerId)
                val lastDownloadInstant = appDB.symbolDownloadTimestampQueries.get(brokerId).executeAsOneOrNull()

                if (lastDownloadInstant != null && !broker.areSymbolsExpired(lastDownloadInstant)) return@forEach

                // Clear previous symbols
                appDB.cachedSymbolQueries.clearByBroker(brokerId)

                broker.downloadSymbols { symbols ->

                    appDB.transaction {

                        for (symbol in symbols) {

                            appDB.cachedSymbolQueries.insert(
                                id = symbol.id,
                                brokerId = brokerId,
                                exchange = symbol.exchange,
                                exchangeToken = symbol.exchangeToken,
                                instrument = symbol.instrument,
                                ticker = symbol.ticker,
                                tickSize = symbol.tickSize,
                                lotSize = symbol.lotSize,
                                description = symbol.description,
                                expiry = symbol.expiry,
                                strikePrice = symbol.strikePrice,
                                optionType = symbol.optionType,
                            )
                        }
                    }
                }

                // Save download timestamp
                appDB.symbolDownloadTimestampQueries.put(brokerId, Clock.System.now())
            }
        }
    }

    override suspend fun getSymbolsFilteredPagingSourceFactory(
        filterQuery: String?,
        instruments: List<Instrument>,
        exchange: String?,
    ): () -> PagingSource<Int, CachedSymbol> {

        downloadAllLatestSymbols()

        return {

            QueryPagingSource(
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
        }
    }

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
    exchange = exchange,
    exchangeToken = exchangeToken,
    instrument = instrument,
    ticker = ticker,
    lotSize = lotSize,
    description = description,
    expiry = expiry,
    strikePrice = strikePrice,
    optionType = optionType,
)

fun SymbolsProvider.getSymbolOrError(
    brokerId: BrokerId,
    symbolId: SymbolId,
): Flow<CachedSymbol> = getSymbol(brokerId, symbolId).map { it ?: error("Symbol (${symbolId.value}) not found") }

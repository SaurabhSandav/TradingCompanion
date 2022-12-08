package trading.data

import AppModule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlinx.serialization.json.*
import trading.*
import kotlin.io.path.*

internal class CandleRepository(
    appModule: AppModule,
    private val candleDownloader: CandleDownloader = FyersCandleDownloader(appModule),
    private val candleCache: CandleCache = CandleCacheDB(appModule),
) {

    suspend fun getCandles(
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Result<CandleSeries, Error> = withContext(Dispatchers.IO) {

        val availableRange = candleCache.getAvailableCandleRange(symbol, timeframe)

        fun ClosedRange<Instant>.prevCandleInstant() = start.minus(timeframe.seconds, DateTimeUnit.SECOND)
        fun ClosedRange<Instant>.nextCandleInstant() = endInclusive.plus(timeframe.seconds, DateTimeUnit.SECOND)

        @Suppress("SpellCheckingInspection")
        val downloadRanges = when {
            // No candles available
            availableRange == null -> listOf(from..to)
            // Some candles available
            // ..........F........T..........
            // ................CCCCCCCCC.....
            from !in availableRange && to in availableRange -> listOf(from..availableRange.prevCandleInstant())
            // Some candles available
            // ..........F........T..........
            // .....CCCCCCCCC................
            from in availableRange && to !in availableRange -> listOf(availableRange.nextCandleInstant()..to)
            // Some candles available between range
            from !in availableRange && to !in availableRange -> {
                when {
                    // ..........F........T..........
                    // ....CCC.......................
                    availableRange.endInclusive < from -> listOf(availableRange.nextCandleInstant()..from)
                    // ..........F........T..........
                    // .......................CCC....
                    availableRange.start > to -> listOf(to..availableRange.prevCandleInstant())
                    // ..........F........T..........
                    // .............CCCC.............
                    else -> listOf(
                        from..availableRange.prevCandleInstant(),
                        availableRange.nextCandleInstant()..to,
                    )
                }
            }
            // All candles available
            // ..........F........T..........
            // .....CCCCCCCCCCCCCCCCCCCC.....
            else -> emptyList()
        }

        downloadRanges.forEach { range ->
            when (val result = download(symbol, timeframe, range.start, range.endInclusive)) {
                is Ok -> candleCache.writeCandles(symbol, timeframe, result.value)
                is Err -> return@withContext when (val error = result.error) {
                    is CandleDownloader.Error.AuthError -> Err(Error.AuthError(error.message))
                    is CandleDownloader.Error.UnknownError -> Err(Error.UnknownError(error.message))
                }
            }
        }

        val candleSeries = MutableCandleSeries(
            initial = candleCache.fetch(symbol, timeframe, from, to),
            timeframe = timeframe,
        ).asCandleSeries()

        return@withContext Ok(candleSeries)
    }

    private suspend fun download(
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Result<List<Candle>, CandleDownloader.Error> {

        val currentTime = Clock.System.now()
        val correctedTo = if (currentTime < to) currentTime else to

        require(from < currentTime) { "Candle Download: from must be before current time" }
        require(from < correctedTo) { "Candle Download: from must be less than to" }

        // Download candles
        val candles = when (val result = candleDownloader.download(symbol, timeframe, from, correctedTo)) {
            is Err -> return result
            is Ok -> result.value
        }

        // Save checked range to avoid re-attempt at downloading already checked range
        candleCache.saveCheckedRange(symbol, timeframe, from, correctedTo)

        // Success
        return Ok(candles)
    }

    sealed class Error {

        abstract val message: String?

        class AuthError(override val message: String?) : Error()

        class UnknownError(override val message: String) : Error()
    }
}

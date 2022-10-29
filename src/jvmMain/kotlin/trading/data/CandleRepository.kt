package trading.data

import AppModule
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlinx.serialization.json.*
import trading.Candle
import trading.CandleSeries
import trading.Timeframe
import utils.AppPaths
import kotlin.io.path.*
import kotlin.time.Duration.Companion.days

internal class CandleRepository(
    appModule: AppModule,
    private val candleDownloader: CandleDownloader = FyersCandleDownloader(appModule),
    private val candleCache: CandleCache = CandleCacheImpl(),
) {

    suspend fun getCandles(
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Result<CandleSeries, Error> {

        // Build directory path for symbol and timeframe
        val baseDir = Path(AppPaths.getAppDataPath())
        val symbolDir = baseDir.resolve("Candles/$symbol/${timeframe.name}")

        // Create directories if not exists
        if (!symbolDir.exists()) symbolDir.createDirectories()

        val availableRange = candleCache.getAvailableCandleRange(symbolDir)

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
                is Ok -> candleCache.writeCandles(symbolDir, result.value)
                is Err -> return when (val error = result.error) {
                    is CandleDownloader.Error.AuthError -> Err(Error.AuthError(error.message))
                    is CandleDownloader.Error.UnknownError -> Err(Error.UnknownError(error.message))
                }
            }
        }

        val candleSeries = CandleSeries(candleCache.fetch(symbolDir, from, to))

        return Ok(candleSeries)
    }

    private suspend fun download(
        symbol: String,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Result<List<Candle>, CandleDownloader.Error> {

        val currentTime = Clock.System.now()
        val correctedTo = if (currentTime < to) currentTime else to
        val downloadInterval = DownloadIntervalDays.days
        val requestedInterval = correctedTo - from
        val candles = mutableListOf<Candle>()

        var currentFrom = from
        var currentTo = if (requestedInterval > downloadInterval) from + downloadInterval else correctedTo

        while (currentTo <= correctedTo && currentFrom != currentTo) {

            when (val result = candleDownloader.download(symbol, timeframe, currentFrom, currentTo)) {
                is Err -> return result
                is Ok -> candles.addAll(result.value)
            }

            // API Rate limit
            delay(400)

            currentFrom = currentTo
            val newCurrentTo = currentFrom + downloadInterval
            currentTo = if (newCurrentTo > correctedTo) correctedTo else newCurrentTo
        }

        return Ok(candles)
    }

    sealed class Error {

        abstract val message: String?

        class AuthError(override val message: String?) : Error()

        class UnknownError(override val message: String) : Error()
    }
}

private const val DownloadIntervalDays = 31

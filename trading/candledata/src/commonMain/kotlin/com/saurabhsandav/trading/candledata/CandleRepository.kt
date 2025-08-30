package com.saurabhsandav.trading.candledata

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.asErr
import com.github.michaelbull.result.fold
import com.saurabhsandav.trading.core.Candle
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.core.Timeframe
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class CandleRepository(
    private val candleDownloader: CandleDownloader,
    private val candleCache: CandleCache,
) {

    fun isLoggedIn(): Flow<Boolean> = candleDownloader.isLoggedIn()

    suspend fun getCountInRange(
        symbolId: SymbolId,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Result<Flow<Long>, Error> {

        // Download entire range / Fill gaps at ends of cached range (if necessary)
        val fillResult = checkAndFillRange(symbolId, timeframe, from, to)
        if (fillResult.isErr) return fillResult.asErr()

        // Fetch and return candles
        return Ok(candleCache.getCountInRange(symbolId, timeframe, from, to))
    }

    suspend fun getInstantBeforeByCount(
        symbolId: SymbolId,
        timeframe: Timeframe,
        before: Instant,
        count: Int,
    ): Result<Flow<Instant?>, Error> {

        // Download entire range / Fill gaps at ends of cached range (if necessary)
        val fillResult = checkAndFillRangeByCount(symbolId, timeframe, before, count, 0)
        if (fillResult.isErr) return fillResult.asErr()

        // Fetch and return candles
        return Ok(candleCache.getInstantBeforeByCount(symbolId, timeframe, before, count))
    }

    suspend fun getInstantAfterByCount(
        symbolId: SymbolId,
        timeframe: Timeframe,
        after: Instant,
        count: Int,
    ): Result<Flow<Instant?>, Error> {

        // Download entire range / Fill gaps at ends of cached range (if necessary)
        val fillResult = checkAndFillRangeByCount(symbolId, timeframe, after, 0, count)
        if (fillResult.isErr) return fillResult.asErr()

        // Fetch and return candles
        return Ok(candleCache.getInstantAfterByCount(symbolId, timeframe, after, count))
    }

    /**
     * Get candles for [symbolId] and [timeframe] in [from]..[to] interval.
     * Set [includeFromCandle] = true to include candle containing [from].
     */
    suspend fun getCandles(
        symbolId: SymbolId,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
        includeFromCandle: Boolean,
    ): Result<Flow<List<Candle>>, Error> {

        // Download entire range / Fill gaps at ends of cached range (if necessary)
        val fillResult = checkAndFillRange(symbolId, timeframe, from, to)
        if (fillResult.isErr) return fillResult.asErr()

        // Fetch and return candles
        return Ok(candleCache.fetchRange(symbolId, timeframe, from, to, includeFromCandle))
    }

    /**
     * Get [count] candles after [at] for [symbolId] and [timeframe].
     * Set [includeAt] = true, to include candle with [Candle.openInstant] = [at] in count.
     */
    suspend fun getCandlesBefore(
        symbolId: SymbolId,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): Result<Flow<List<Candle>>, Error> {

        // Download entire range / Fill gaps at ends of cached range (if necessary)
        val fillResult = checkAndFillRangeByCount(symbolId, timeframe, at, count, 0)
        if (fillResult.isErr) return fillResult.asErr()

        // Fetch and return candles
        return Ok(candleCache.getBefore(symbolId, timeframe, at, count, includeAt))
    }

    /**
     * Get [count] candles before [at] for [symbolId] and [timeframe].
     * Set [includeAt] = true, to include candle with [Candle.openInstant] = [at] in count.
     */
    suspend fun getCandlesAfter(
        symbolId: SymbolId,
        timeframe: Timeframe,
        at: Instant,
        count: Int,
        includeAt: Boolean,
    ): Result<Flow<List<Candle>>, Error> {

        // Download entire range / Fill gaps at ends of cached range (if necessary)
        val fillResult = checkAndFillRangeByCount(symbolId, timeframe, at, 0, count)
        if (fillResult.isErr) return fillResult.asErr()

        // Fetch and return candles
        return Ok(candleCache.getAfter(symbolId, timeframe, at, count, includeAt))
    }

    private suspend fun checkAndFillRange(
        symbolId: SymbolId,
        timeframe: Timeframe,
        from: Instant,
        to: Instant,
    ): Result<Unit, Error> {

        val availableRange = candleCache.getCheckedRange(symbolId, timeframe)

        fun ClosedRange<Instant>.startBufferInstant() = start + timeframe.seconds.seconds

        fun ClosedRange<Instant>.endBufferInstant() = endInclusive - timeframe.seconds.seconds

        @Suppress("SpellCheckingInspection")
        val downloadRanges = when {
            // No candles available
            availableRange == null -> listOf(from..to)
            // Some candles available
            // ..........F........T..........
            // ................CCCCCCCCC.....
            from !in availableRange && to in availableRange -> listOf(from..availableRange.startBufferInstant())
            // Some candles available
            // ..........F........T..........
            // .....CCCCCCCCC................
            from in availableRange && to !in availableRange -> listOf(availableRange.endBufferInstant()..to)
            // Some candles available between range
            from !in availableRange && to !in availableRange -> {
                when {
                    // ..........F........T..........
                    // ....CCC.......................
                    availableRange.endInclusive < from -> listOf(availableRange.endBufferInstant()..to)
                    // ..........F........T..........
                    // .......................CCC....
                    availableRange.start > to -> listOf(from..availableRange.startBufferInstant())
                    // ..........F........T..........
                    // .............CCCC.............
                    else -> listOf(
                        from..availableRange.startBufferInstant(),
                        availableRange.endBufferInstant()..to,
                    )
                }
            }
            // All candles available
            // ..........F........T..........
            // .....CCCCCCCCCCCCCCCCCCCC.....
            else -> emptyList()
        }

        downloadRanges.filterNot { range -> range.start > range.endInclusive }.forEach { range ->

            val result = download(symbolId, timeframe, range)

            result.fold(
                success = { candles ->
                    // Replace candles in given range.
                    candleCache.replace(symbolId, timeframe, range, candles)
                },
                failure = { error ->
                    return when (error) {
                        is CandleDownloader.Error.AuthError -> Err(Error.AuthError(error.message))
                        is CandleDownloader.Error.UnknownError -> Err(Error.UnknownError(error.message))
                    }
                },
            )
        }

        return Ok(Unit)
    }

    private suspend fun checkAndFillRangeByCount(
        symbolId: SymbolId,
        timeframe: Timeframe,
        at: Instant,
        before: Int,
        after: Int,
    ): Result<Unit, Error> {

        val downloadInterval = 365.days

        // Check and fill range around $at by default
        // If $at is outside an already cached range, this will fill the gaps between $at and the range.
        val fillResult = checkAndFillRange(
            symbolId = symbolId,
            timeframe = timeframe,
            from = at - downloadInterval,
            to = at + downloadInterval,
        )
        if (fillResult.isErr) return fillResult

        val initialCountRange = candleCache.getCountAt(symbolId, timeframe, at)!!

        // Not enough candles available before $at
        if (initialCountRange.beforeCount < before) {

            var currentCountRange = initialCountRange

            while (currentCountRange.beforeCount < before) {

                // Current first available candle open time
                val firstCandleInstant = currentCountRange.firstCandleInstant

                // Download given interval's worth before the first available candle
                val beforeFillResult = checkAndFillRange(
                    symbolId = symbolId,
                    timeframe = timeframe,
                    from = (firstCandleInstant ?: at) - downloadInterval,
                    to = at,
                )
                if (beforeFillResult.isErr) return beforeFillResult

                // Count range after download
                val newCountRange = candleCache.getCountAt(symbolId, timeframe, at)!!

                // If no new candles downloaded, no new candles are available. Stop trying to download more.
                if (newCountRange.firstCandleInstant == firstCandleInstant) break

                // Update count range for next loop
                currentCountRange = newCountRange
            }
        }

        // Not enough candles available after $at
        if (initialCountRange.afterCount < after) {

            var currentCountRange = initialCountRange

            while (currentCountRange.afterCount < after) {

                // Current last available candle open time
                val lastCandleInstant = currentCountRange.lastCandleInstant

                // Download given interval's worth after the last available candle
                val afterFillResult = checkAndFillRange(
                    symbolId = symbolId,
                    timeframe = timeframe,
                    from = at,
                    to = (lastCandleInstant ?: at) + downloadInterval,
                )
                if (afterFillResult.isErr) return afterFillResult

                // Count range after download
                val newCountRange = candleCache.getCountAt(symbolId, timeframe, at)!!

                // If no new candles downloaded, no new candles are available. Stop trying to download more.
                if (newCountRange.lastCandleInstant == lastCandleInstant) break

                // Update count range for next loop
                currentCountRange = newCountRange
            }
        }

        return Ok(Unit)
    }

    private suspend fun download(
        symbolId: SymbolId,
        timeframe: Timeframe,
        range: ClosedRange<Instant>,
    ): Result<List<Candle>, CandleDownloader.Error> {

        val from = range.start
        val to = range.endInclusive
        val currentTime = Clock.System.now()
        val correctedTo = if (currentTime < to) currentTime else to

        // Invalid range, return success with no candles
        if (from == correctedTo || from > currentTime) return Ok(emptyList())

        // Download candles
        val result = candleDownloader.download(symbolId, timeframe, from, correctedTo)
        val candles = result.fold(
            success = { candles -> candles },
            failure = { error -> return Err(error) },
        )

        // Save checked range to avoid re-attempt at downloading already checked range
        candleCache.saveCheckedRange(
            symbolId = symbolId,
            timeframe = timeframe,
            from = from,
            to = run {

                // If correctedTo is today, set checked range to end at start of day, today.
                // This will re-fetch all the candles for today everytime they're requested.
                // This handles situations where incorrect candles are provided by the download service.
                // The assumption is that the candles will be corrected by the end of day.
                val today = currentTime.toLocalDateTime(TimeZone.currentSystemDefault()).date
                val correctedDate = correctedTo.toLocalDateTime(TimeZone.currentSystemDefault()).date

                when {
                    today == correctedDate -> today.atStartOfDayIn(TimeZone.currentSystemDefault())
                    else -> correctedTo
                }
            },
        )

        // Success
        return Ok(candles)
    }

    sealed class Error {

        abstract val message: String?

        class AuthError(
            override val message: String?,
        ) : Error()

        class UnknownError(
            override val message: String,
        ) : Error()
    }
}

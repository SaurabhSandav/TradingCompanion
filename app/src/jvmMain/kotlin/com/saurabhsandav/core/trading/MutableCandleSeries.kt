package com.saurabhsandav.core.trading

import com.saurabhsandav.core.trading.indicator.base.IndicatorCache
import com.saurabhsandav.core.utils.removeFirst
import com.saurabhsandav.core.utils.removeLast
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import java.math.MathContext
import java.math.RoundingMode

interface MutableCandleSeries : CandleSeries {

    fun addLiveCandle(candle: Candle)

    fun appendCandles(candles: List<Candle>)

    fun prependCandles(candles: List<Candle>)

    fun replaceCandles(candles: List<Candle>)

    fun removeFirst(n: Int = 1)

    fun removeLast(n: Int = 1)

    fun clear()

    companion object {

        operator fun invoke(
            initial: List<Candle> = emptyList(),
            timeframe: Timeframe,
            maxCandleCount: Int = Int.MAX_VALUE,
            indicatorMathContext: MathContext = MathContext(
                20,
                RoundingMode.HALF_EVEN,
            ),
        ): MutableCandleSeries = MutableCandleSeriesImpl(
            initial = initial,
            timeframe = timeframe,
            maxCandleCount = maxCandleCount,
            indicatorMathContext = indicatorMathContext,
        )
    }
}

private class MutableCandleSeriesImpl(
    initial: List<Candle>,
    override val timeframe: Timeframe,
    private val maxCandleCount: Int,
    override val indicatorMathContext: MathContext,
    private val list: MutableList<Candle> = mutableListOf(),
) : MutableCandleSeries,
    List<Candle> by list {

    private val indicatorCaches = mutableListOf<IndicatorCache<*>>()

    private val _live = MutableSharedFlow<IndexedValue<Candle>>(extraBufferCapacity = Int.MAX_VALUE)
    override val live: Flow<IndexedValue<Candle>> = _live.asSharedFlow()

    private val _modifications = MutableSharedFlow<Pair<ClosedRange<Instant>?, ClosedRange<Instant>?>>(
        replay = 1,
        extraBufferCapacity = Int.MAX_VALUE,
    )
    override val modifications: Flow<Pair<ClosedRange<Instant>?, ClosedRange<Instant>?>> = _modifications.asSharedFlow()

    private val _instantRange = MutableStateFlow<ClosedRange<Instant>?>(null)
    override val instantRange: StateFlow<ClosedRange<Instant>?> = _instantRange.asStateFlow()

    init {
        appendCandles(initial)
    }

    override fun addLiveCandle(candle: Candle) {

        appendCandle(candle)

        // Update live flow
        _live.tryEmit(IndexedValue(lastIndex, candle))

        // Update instant range
        _instantRange.value = when {
            list.isNotEmpty() -> first().openInstant..last().openInstant
            else -> null
        }
    }

    override fun appendCandles(candles: List<Candle>) {

        if (candles.isEmpty()) return

        val prevInstantRange = instantRange.value

        candles.forEach(::appendCandle)

        // Update instant range
        _instantRange.value = when {
            list.isNotEmpty() -> first().openInstant..last().openInstant
            else -> null
        }

        // Notify modification
        val newInstantRange = instantRange.value
        _modifications.tryEmit(prevInstantRange to newInstantRange)
    }

    private fun appendCandle(candle: Candle) {

        val lastCandle = list.lastOrNull()

        if (lastCandle != null && lastCandle.openInstant > candle.openInstant) {
            error(
                """
                |Candle cannot be older than the last candle in the series: 
                |New Candle: $candle
                |Last Candle: $lastCandle
                |
                """.trimMargin(),
            )
        }

        val isCandleUpdate = lastCandle?.openInstant == candle.openInstant

        when {
            isCandleUpdate -> {

                // Replace candle
                list[list.lastIndex] = candle

                // Reset indicator value
                indicatorCaches.forEach { it[list.lastIndex] = null }
            }

            else -> {

                // Append candle
                list.add(candle)

                // If series size is greater than max candle count,
                // drop first candle and first indicator cache values
                if (list.size > maxCandleCount) {
                    list.removeFirst()
                    indicatorCaches.forEach { it.removeFirst() }
                }
            }
        }
    }

    override fun prependCandles(candles: List<Candle>) {

        if (candles.isEmpty()) return

        val prevInstantRange = instantRange.value

        candles.asReversed().forEach(::prependCandle)

        // Recalculate all indicator values
        indicatorCaches.forEach(IndicatorCache<*>::clear)

        // Update instant range
        _instantRange.value = when {
            list.isNotEmpty() -> first().openInstant..last().openInstant
            else -> null
        }

        // Notify modification
        val newInstantRange = instantRange.value
        _modifications.tryEmit(prevInstantRange to newInstantRange)
    }

    private fun prependCandle(candle: Candle) {

        val firstCandle = list.firstOrNull()

        if (firstCandle != null && firstCandle.openInstant < candle.openInstant) {
            error(
                """
                |Candle cannot be newer than the oldest candle in the series: 
                |New Candle: $candle
                |Oldest Candle: $firstCandle
                |
                """.trimMargin(),
            )
        }

        val isCandleUpdate = firstCandle?.openInstant == candle.openInstant

        when {
            // Replace Candle
            isCandleUpdate -> list[0] = candle
            else -> {

                // Prepend candle
                list.add(0, candle)

                // If series size is greater than max candle count, drop last candle
                if (list.size > maxCandleCount) list.removeLast()
            }
        }
    }

    override fun replaceCandles(candles: List<Candle>) {

        val prevInstantRange = instantRange.value

        // Clear cached indicators
        indicatorCaches.forEach { it.clear() }

        // Remove all candles
        list.clear()

        candles.forEach(::appendCandle)

        // Update instant range
        _instantRange.value = when {
            list.isNotEmpty() -> first().openInstant..last().openInstant
            else -> null
        }

        // Notify modification
        val newInstantRange = instantRange.value
        _modifications.tryEmit(prevInstantRange to newInstantRange)
    }

    override fun removeFirst(n: Int) {

        val prevInstantRange = instantRange.value

        // Drop cached indicators values
        indicatorCaches.forEach { it.removeFirst(n) }

        // Remove candles
        list.removeFirst(n)

        // Update instant range
        _instantRange.value = when {
            list.isNotEmpty() -> first().openInstant..last().openInstant
            else -> null
        }

        // Notify modification
        val newInstantRange = instantRange.value
        _modifications.tryEmit(prevInstantRange to newInstantRange)
    }

    override fun removeLast(n: Int) {

        val prevInstantRange = instantRange.value

        // Drop cached indicators values
        indicatorCaches.forEach { it.removeLast(n) }

        // Remove candles
        list.removeLast(n)

        // Update instant range
        _instantRange.value = when {
            list.isNotEmpty() -> first().openInstant..last().openInstant
            else -> null
        }

        // Notify modification
        val newInstantRange = instantRange.value
        _modifications.tryEmit(prevInstantRange to newInstantRange)
    }

    override fun clear() {

        val prevInstantRange = instantRange.value

        // Clear cached indicators
        indicatorCaches.forEach { it.clear() }

        // Remove all candles
        list.clear()

        // Update instant range
        _instantRange.value = null

        // Notify modification
        _modifications.tryEmit(prevInstantRange to null)
    }

    override fun <T> getIndicatorCache(key: Indicator.CacheKey?): IndicatorCache<T> {

        val cache = when (key) {

            // New cache requested
            null -> IndicatorCache<T>(null).also(indicatorCaches::add)

            // Return pre-exiting cache or create new
            else -> when (val cache = indicatorCaches.firstOrNull { key == it.key }) {
                null -> IndicatorCache<T>(key).also(indicatorCaches::add)
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    cache as IndicatorCache<T>
                }
            }
        }

        return cache
    }
}

fun MutableCandleSeries.asCandleSeries(): CandleSeries = object : CandleSeries by this {}

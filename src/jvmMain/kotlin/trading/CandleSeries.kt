package trading

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import trading.indicator.base.IndicatorCache
import java.math.MathContext
import java.math.RoundingMode

class CandleSeries(
    initial: List<Candle> = emptyList(),
    private val maxCandleCount: Int = Int.MAX_VALUE,
    val indicatorMathContext: MathContext = MathContext(
        20,
        RoundingMode.HALF_EVEN,
    ),
    seriesTimeframe: Timeframe? = null,
) {

    private val series = mutableListOf<Candle>()
    private val indicatorCaches = mutableListOf<IndicatorCache<*>>()

    private val _live = MutableStateFlow(initial.lastOrNull())
    val live: StateFlow<Candle?> = _live.asStateFlow()

    var timeframe: Timeframe? = seriesTimeframe

    init {

        // Determine timeframe
        if (initial.size >= 2)
            timeframe = calculateTimeframe(initial[initial.lastIndex - 1], initial.last())

        initial.forEach { addCandle(it) }
    }

    val list: List<Candle>
        get() = series

    fun addCandle(candle: Candle) {

        val lastCandle = series.lastOrNull()

        // Determine timeframe
        if (timeframe == null && lastCandle != null) timeframe = calculateTimeframe(lastCandle, candle)

        if (lastCandle != null && lastCandle.openInstant > candle.openInstant)
            error("Candle cannot be older than the last candle in the series: $candle")

        val isCandleUpdate = lastCandle?.openInstant == candle.openInstant

        if (isCandleUpdate) {

            val updateIndex = series.lastIndex

            series.removeLast()
            series.add(candle)

            // Drop cached indicators values at index
            indicatorCaches.forEach { it[updateIndex] = null }
        } else {

            series.add(candle)

            // If series size is greater than max candle count,
            // drop first candle and first indicator cache values
            if (series.size > maxCandleCount) {
                series.removeFirst()
                indicatorCaches.forEach { it.shrink() }
            }
        }

        _live.value = candle
    }

    internal fun <T> getIndicatorCache(key: String?): IndicatorCache<T> {

        val cache = when (key) {

            // New cache requested
            null -> IndicatorCache(null)

            // Return pre-exiting cache or create new
            else -> when (val cache = indicatorCaches.firstOrNull { key == it.key }) {
                null -> IndicatorCache<T>(key).also { indicatorCaches.add(it) }
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    cache as IndicatorCache<T>
                }
            }
        }

        return cache
    }

    private fun calculateTimeframe(
        candle: Candle,
        nextCandle: Candle,
    ): Timeframe {

        val diff = nextCandle.openInstant - candle.openInstant

        return Timeframe.fromSeconds(diff.inWholeSeconds) ?: error("Unknown Timeframe")
    }
}

package trading

import kotlinx.coroutines.flow.*
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
    val timeframe: Timeframe? = null,
) {

    private val series = mutableListOf<Candle>()
    private val indicatorCaches = mutableListOf<IndicatorCache<*>>()

    private val _live = MutableSharedFlow<Candle>(extraBufferCapacity = Int.MAX_VALUE)
    val live: Flow<Candle> = _live.asSharedFlow()

    init {
        initial.forEach(::addCandle)
    }

    val list: List<Candle>
        get() = series

    fun addCandle(candle: Candle) {

        val lastCandle = series.lastOrNull()

        if (lastCandle != null && lastCandle.openInstant > candle.openInstant)
            error(
                "Candle cannot be older than the last candle in the series: " +
                        "\nNew Candle: $candle \nLast Candle: $lastCandle"
            )

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
                indicatorCaches.forEach(IndicatorCache<*>::shrink)
            }
        }

        _live.tryEmit(candle)
    }

    internal fun <T> getIndicatorCache(key: String?): IndicatorCache<T> {

        val cache = when (key) {

            // New cache requested
            null -> IndicatorCache(null)

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

package trading

import kotlinx.coroutines.flow.*
import trading.indicator.base.IndicatorCache
import java.math.MathContext
import java.math.RoundingMode

class CandleSeries private constructor(
    initial: List<Candle>,
    private val maxCandleCount: Int,
    val indicatorMathContext: MathContext,
    val timeframe: Timeframe?,
    private val list: MutableList<Candle>,
) : List<Candle> by list {

    constructor(
        initial: List<Candle> = emptyList(),
        maxCandleCount: Int = Int.MAX_VALUE,
        indicatorMathContext: MathContext = MathContext(
            20,
            RoundingMode.HALF_EVEN,
        ),
        timeframe: Timeframe? = null,
    ) : this(
        initial = initial,
        maxCandleCount = maxCandleCount,
        indicatorMathContext = indicatorMathContext,
        timeframe = timeframe,
        list = mutableListOf()
    )

    private val indicatorCaches = mutableListOf<IndicatorCache<*>>()

    private val _live = MutableSharedFlow<Candle>(extraBufferCapacity = Int.MAX_VALUE)
    val live: Flow<Candle> = _live.asSharedFlow()

    init {
        initial.forEach(::addCandle)
    }

    fun addCandle(candle: Candle) {

        val lastCandle = list.lastOrNull()

        if (lastCandle != null && lastCandle.openInstant > candle.openInstant)
            error(
                "Candle cannot be older than the last candle in the series: " +
                        "\nNew Candle: $candle \nLast Candle: $lastCandle"
            )

        val isCandleUpdate = lastCandle?.openInstant == candle.openInstant

        if (isCandleUpdate) {

            val updateIndex = list.lastIndex

            list.removeLast()
            list.add(candle)

            // Drop cached indicators values at index
            indicatorCaches.forEach { it[updateIndex] = null }
        } else {

            list.add(candle)

            // If series size is greater than max candle count,
            // drop first candle and first indicator cache values
            if (list.size > maxCandleCount) {
                list.removeFirst()
                indicatorCaches.forEach(IndicatorCache<*>::shrink)
            }
        }

        _live.tryEmit(candle)
    }

    fun removeLast(n: Int = 1) {

        repeat(n) {

            val removeIndex = list.lastIndex

            list.removeLast()

            // Drop cached indicators values at index
            indicatorCaches.forEach { it[removeIndex] = null }
        }
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

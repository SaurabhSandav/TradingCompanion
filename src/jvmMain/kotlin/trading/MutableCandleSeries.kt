package trading

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import trading.indicator.base.IndicatorCache
import java.math.MathContext
import java.math.RoundingMode

interface MutableCandleSeries : CandleSeries {

    fun addCandle(candle: Candle)

    fun prependCandles(candles: List<Candle>)

    fun removeLast(n: Int = 1)

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
) : MutableCandleSeries, List<Candle> by list {

    private val indicatorCaches = mutableListOf<IndicatorCache<*>>()

    private val _live = MutableSharedFlow<Candle>(extraBufferCapacity = Int.MAX_VALUE)
    override val live: Flow<Candle> = _live.asSharedFlow()

    init {
        initial.forEach(::addCandle)
    }

    override fun addCandle(candle: Candle) {

        val lastCandle = list.lastOrNull()

        if (lastCandle != null && lastCandle.openInstant > candle.openInstant)
            error(
                """
                    |Candle cannot be older than the last candle in the series: 
                    |New Candle: $candle
                    |Last Candle: $lastCandle
                """.trimMargin()
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

    override fun prependCandles(candles: List<Candle>) {

        candles.asReversed().forEach(::prependCandle)

        // Recalculate all indicator values
        indicatorCaches.forEach { it.clear() }
    }

    private fun prependCandle(candle: Candle) {

        val firstCandle = list.firstOrNull()

        if (firstCandle != null && firstCandle.openInstant < candle.openInstant) {
            error(
                """
                |Candle cannot be newer than the oldest candle in the series: 
                |New Candle: $candle
                |Current oldest Candle: $firstCandle
                """.trimMargin()
            )
        }

        // If series size is greater than max candle count,
        // drop first candle and first indicator cache values
        if (list.size > maxCandleCount) {
            error("maxCandleCount exceeded, cannot add candle.")
        }

        val isCandleUpdate = firstCandle?.openInstant == candle.openInstant

        if (isCandleUpdate) {
            list.removeAt(0)
        }

        list.add(0, candle)
    }

    override fun removeLast(n: Int) {

        repeat(n) {

            val removeIndex = list.lastIndex

            list.removeLast()

            // Drop cached indicators values at index
            indicatorCaches.forEach { it[removeIndex] = null }
        }
    }

    override fun <T> getIndicatorCache(key: String?): IndicatorCache<T> {

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

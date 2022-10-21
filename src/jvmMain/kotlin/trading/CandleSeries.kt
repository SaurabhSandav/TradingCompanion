package trading

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import trading.indicator.base.IndicatorCache
import java.math.MathContext
import java.math.RoundingMode

class CandleSeries(
    private val maxCandleCount: Int = Int.MAX_VALUE,
    initial: List<Candle> = emptyList(),
    val indicatorMathContext: MathContext = MathContext(
        20,
        RoundingMode.HALF_EVEN,
    ),
) : List<Candle> {

    private val series = mutableListOf<Candle>()
    private val indicatorCaches = mutableListOf<IndicatorCache<*>>()

    private val _live = MutableStateFlow(initial.lastOrNull())
    val live: StateFlow<Candle?> = _live.asStateFlow()

    init {
        initial.forEach { addCandle(it) }
    }

    fun addCandle(candle: Candle) {

        val isCandleUpdate = series.lastOrNull()?.openInstant == candle.openInstant

        if (isCandleUpdate) {

            val updateIndex = series.lastIndex

            series.removeLast()
            series.add(candle)

            // Drop cached indicators values at index
            indicatorCaches.forEach { it[updateIndex] = null }
        } else {

            series.add(candle)

            // Drop first candle and first indicator cache values
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

    // region List overrides

    override val size: Int
        get() = series.size

    override fun contains(element: Candle): Boolean = series.contains(element)

    override fun containsAll(elements: Collection<Candle>): Boolean = series.containsAll(elements)

    override fun get(index: Int): Candle = series[index]

    override fun indexOf(element: Candle): Int = series.indexOf(element)

    override fun isEmpty(): Boolean = series.isEmpty()

    override fun iterator(): Iterator<Candle> = series.iterator()

    override fun lastIndexOf(element: Candle): Int = series.lastIndexOf(element)

    override fun listIterator(): ListIterator<Candle> = series.listIterator()

    override fun listIterator(index: Int): ListIterator<Candle> = series.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<Candle> = series.subList(fromIndex, toIndex)

    // endregion List overrides
}

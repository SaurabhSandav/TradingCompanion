package trading.indicator.base

class IndicatorCache<T> internal constructor(val key: String?) {

    private val cache = mutableListOf<T?>()

    internal operator fun get(index: Int): T? {
        return cache.getOrNull(index)
    }

    internal operator fun set(index: Int, value: T?) {

        if (cache.lastIndex < index) {
            val incrementSize = index - cache.lastIndex
            repeat(incrementSize) { cache.add(null) }
        }

        cache[index] = value
    }

    internal fun shrink() {
        cache.removeFirst()
    }

    internal fun clear() {
        cache.clear()
    }
}

package com.saurabhsandav.core.trading.core

class IndicatorCache<T> internal constructor(
    val key: Indicator.CacheKey?,
) {

    private val cache = mutableListOf<T?>()

    internal operator fun get(index: Int): T? {
        return cache.getOrNull(index)
    }

    internal operator fun set(
        index: Int,
        value: T?,
    ) {

        if (index !in cache.indices) {
            val incrementSize = index - cache.lastIndex
            repeat(incrementSize) { cache.add(null) }
        }

        cache[index] = value
    }

    internal fun removeFirst(n: Int = 1) {
        cache.removeFirst(n)
    }

    internal fun removeLast(n: Int = 1) {
        cache.removeLast(n)
    }

    internal fun clear() {
        cache.clear()
    }
}

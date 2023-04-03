package com.saurabhsandav.core.trading.indicator.base

class IndicatorCache<T> internal constructor(val key: String?) {

    private val cache = mutableListOf<T?>()

    internal operator fun get(index: Int): T? {
        return cache.getOrNull(index)
    }

    internal operator fun set(index: Int, value: T?) {

        if (index !in cache.indices) {
            val incrementSize = index - cache.lastIndex
            repeat(incrementSize) { cache.add(null) }
        }

        cache[index] = value
    }

    internal fun removeAt(index: Int) {
        if (index in cache.indices) cache.removeAt(index)
    }

    internal fun clear() {
        cache.clear()
    }
}

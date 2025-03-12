package com.saurabhsandav.core.trading.indicator.base

import com.saurabhsandav.core.trading.Indicator
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun buildIndicatorCacheKey(block: IndicatorCacheKeyBuilderScope.() -> Indicator.CacheKey): Indicator.CacheKey? {

    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    val scope = IndicatorCacheKeyBuilderScope()

    return try {
        scope.block()
    } catch (_: NoKeyException) {
        null
    }
}

class IndicatorCacheKeyBuilderScope {

    fun Indicator<*>.bindCacheKey(): Indicator.CacheKey {
        return cacheKey ?: throw NoKeyException()
    }
}

private class NoKeyException : Exception() {

    override fun fillInStackTrace(): Throwable = this
}

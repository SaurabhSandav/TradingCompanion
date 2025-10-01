package com.saurabhsandav.trading.core

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
        return cacheKey ?: throwNoKeyException()
    }
}

internal abstract class NoKeyException : Exception()

internal expect fun throwNoKeyException(): Nothing

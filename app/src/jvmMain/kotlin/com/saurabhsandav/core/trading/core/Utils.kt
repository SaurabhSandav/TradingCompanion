package com.saurabhsandav.core.trading.core

internal fun <E> MutableList<E>.removeFirst(n: Int = 1) {
    subList(
        fromIndex = 0,
        toIndex = (n + 1).coerceAtMost(size),
    ).clear()
}

internal fun <E> MutableList<E>.removeLast(n: Int = 1) {
    subList(
        fromIndex = (size - n).coerceAtLeast(0),
        toIndex = size,
    ).clear()
}

package com.saurabhsandav.core.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <E> MutableList<E>.removeFirst(n: Int = 1) {
    subList(
        fromIndex = 0,
        toIndex = (n + 1).coerceAtMost(size),
    ).clear()
}

fun <E> MutableList<E>.removeLast(n: Int = 1) {
    subList(
        fromIndex = (size - n).coerceAtLeast(0),
        toIndex = size,
    ).clear()
}

internal inline fun <T, R> Flow<List<T>>.mapList(
    crossinline transform: suspend (value: T) -> R,
): Flow<List<R>> = transform { list ->
    return@transform emit(list.map { transform(it) })
}

fun CoroutineScope.launchUnit(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
) {
    launch(context, start, block)
}

fun CoroutineScope.newChildScope(): CoroutineScope = this + Job(parent = coroutineContext[Job])

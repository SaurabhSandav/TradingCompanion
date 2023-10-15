package com.saurabhsandav.core.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndexExclusive] (exclusive).
 * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list, and vice-versa.
 *
 * Structural changes in the base list make the behavior of the view undefined.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun <E> List<E>.subList(fromIndex: Int, toIndexExclusive: Int): List<E> {
    return subList(fromIndex, toIndexExclusive)
}

/**
 * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (inclusive).
 * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list, and vice-versa.
 *
 * Structural changes in the base list make the behavior of the view undefined.
 */
fun <E> List<E>.subListInclusive(fromIndex: Int, toIndex: Int): List<E> {
    return subList(fromIndex, toIndex + 1)
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

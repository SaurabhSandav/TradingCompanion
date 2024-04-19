package com.saurabhsandav.core.utils

fun <T : Comparable<T>> List<T?>.binarySearchAsResult(
    element: T?,
    fromIndex: Int = 0,
    toIndex: Int = size,
): BinarySearchResult {

    val searchIndex = binarySearch(
        element = element,
        fromIndex = fromIndex,
        toIndex = toIndex,
    )

    return searchIndex.asResult(fromIndex, toIndex)
}

fun <T> List<T>.binarySearchAsResult(
    element: T,
    comparator: Comparator<in T>,
    fromIndex: Int = 0,
    toIndex: Int = size,
): BinarySearchResult {

    val searchIndex = binarySearch(
        element = element,
        comparator = comparator,
        fromIndex = fromIndex,
        toIndex = toIndex,
    )

    return searchIndex.asResult(fromIndex, toIndex)
}

fun <T> List<T>.binarySearchAsResult(
    fromIndex: Int = 0,
    toIndex: Int = size,
    comparison: (T) -> Int,
): BinarySearchResult {

    val searchIndex = binarySearch(
        fromIndex = fromIndex,
        toIndex = toIndex,
        comparison = comparison,
    )

    return searchIndex.asResult(fromIndex, toIndex)
}

private fun Int.asResult(
    fromIndex: Int,
    toIndex: Int,
): BinarySearchResult {

    val searchIndex = this

    return when {
        searchIndex >= 0 -> BinarySearchResult.Found(searchIndex)
        else -> {

            val naturalIndex = -(searchIndex + 1)

            BinarySearchResult.NotFound(
                naturalIndex = naturalIndex,
                isOutsideRange = naturalIndex == fromIndex || naturalIndex == toIndex + 1,
            )
        }
    }
}

inline fun <T, K : Comparable<K>> List<T>.binarySearchByAsResult(
    key: K?,
    fromIndex: Int = 0,
    toIndex: Int = size,
    crossinline selector: (T) -> K?,
): BinarySearchResult = binarySearchAsResult(fromIndex, toIndex) { compareValues(selector(it), key) }

sealed class BinarySearchResult {

    data class Found(val index: Int) : BinarySearchResult()

    data class NotFound(
        val naturalIndex: Int,
        val isOutsideRange: Boolean,
    ) : BinarySearchResult()
}

val BinarySearchResult.indexOrNaturalIndex: Int
    get() = when (this) {
        is BinarySearchResult.Found -> index
        is BinarySearchResult.NotFound -> naturalIndex
    }

fun BinarySearchResult.indexOr(
    block: (naturalIndex: Int) -> Int,
): Int = when (this) {
    is BinarySearchResult.Found -> index
    is BinarySearchResult.NotFound -> block(naturalIndex)
}

fun BinarySearchResult.indexOrNull(): Int? = when (this) {
    is BinarySearchResult.Found -> index
    is BinarySearchResult.NotFound -> null
}

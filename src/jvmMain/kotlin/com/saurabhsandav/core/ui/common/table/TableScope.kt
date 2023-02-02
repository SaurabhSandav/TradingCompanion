package com.saurabhsandav.core.ui.common.table

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

interface TableScope<T> {

    val schema: TableSchema<T>

    fun row(
        key: Any? = null,
        contentType: Any? = null,
        rowContent: @Composable LazyItemScope.() -> Unit,
    )

    fun rows(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        rowContent: @Composable LazyItemScope.(index: Int) -> Unit,
    )

    fun stickyHeader(
        key: Any? = null,
        contentType: Any? = null,
        rowContent: @Composable LazyItemScope.() -> Unit,
    )
}

class TableScopeImpl<T>(
    private val lazyListScope: LazyListScope,
    override val schema: TableSchema<T>,
) : TableScope<T> {

    override fun row(
        key: Any?,
        contentType: Any?,
        rowContent: @Composable LazyItemScope.() -> Unit,
    ) {
        lazyListScope.item(
            key = key,
            contentType = contentType,
            content = rowContent,
        )
    }

    override fun rows(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        rowContent: @Composable LazyItemScope.(index: Int) -> Unit,
    ) {
        lazyListScope.items(
            count = count,
            key = key,
            contentType = contentType,
            itemContent = rowContent,
        )
    }

    override fun stickyHeader(
        key: Any?,
        contentType: Any?,
        rowContent: @Composable LazyItemScope.() -> Unit,
    ) {
        lazyListScope.stickyHeader(
            key = key,
            contentType = contentType,
            content = rowContent,
        )
    }
}

inline fun <T> TableScope<T>.rows(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline rowContent: @Composable LazyItemScope.(item: T) -> Unit = { item -> DefaultTableRow(item, schema) },
) = rows(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    contentType = { index: Int -> contentType(items[index]) }
) {
    rowContent(items[it])
}

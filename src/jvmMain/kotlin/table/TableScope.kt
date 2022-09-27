package table

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

interface TableScope<T> {

    fun row(
        key: Any? = null,
        contentType: Any? = null,
        rowContent: @Composable TableRowScope<T>.() -> Unit,
    )

    fun rows(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        rowContent: @Composable TableRowScope<T>.(index: Int) -> Unit,
    )

    fun stickyHeader(
        key: Any? = null,
        contentType: Any? = null,
        rowContent: @Composable TableRowScope<T>.() -> Unit,
    )
}

class TableScopeImpl<T>(
    private val lazyListScope: LazyListScope,
    schema: TableSchema<T>,
) : TableScope<T> {

    private val tableRowScope = TableRowScope(schema)

    override fun row(
        key: Any?,
        contentType: Any?,
        rowContent: @Composable TableRowScope<T>.() -> Unit,
    ) {
        lazyListScope.item(key, contentType) {
            tableRowScope.rowContent()
        }
    }

    override fun rows(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        rowContent: @Composable TableRowScope<T>.(index: Int) -> Unit,
    ) {
        lazyListScope.items(count, key, contentType) {
            tableRowScope.rowContent(it)
        }
    }

    override fun stickyHeader(
        key: Any?,
        contentType: Any?,
        rowContent: @Composable TableRowScope<T>.() -> Unit,
    ) {
        lazyListScope.stickyHeader(key, contentType) {
            tableRowScope.rowContent()
        }
    }
}

inline fun <T> TableScope<T>.rows(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline rowContent: @Composable TableRowScope<T>.(item: T) -> Unit = { item -> DefaultRow(item) },
) = rows(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    contentType = { index: Int -> contentType(items[index]) }
) {
    rowContent(items[it])
}

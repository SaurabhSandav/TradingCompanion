package experimental

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import utils.state

@Composable
internal fun <T> Table(
    schema: TableSchema<T>,
    modifier: Modifier = Modifier,
    headerContent: @Composable TableHeaderScope<T>.() -> Unit = { DefaultHeader() },
    content: TableScope<T>.() -> Unit,
) {

    Column(modifier = modifier) {

        val headerScope = remember(schema) { TableHeaderScope(schema) }

        headerScope.headerContent()

        Divider()

        LazyColumn {
            TableScopeImpl(this, schema).content()
        }
    }
}

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

interface TableHeaderScope<T> {

    val schema: TableSchema<T>

    @Composable
    fun DefaultHeader(modifier: Modifier = Modifier) {

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            schema.columns.forEach { column ->
                Box(Modifier.weight(1F)) {
                    column.header?.invoke()
                }
            }
        }
    }


    companion object {
        internal operator fun <T> invoke(schema: TableSchema<T>): TableHeaderScope<T> {
            return object : TableHeaderScope<T> {
                override val schema: TableSchema<T> = schema
            }
        }
    }
}

interface TableRowScope<T> {

    val schema: TableSchema<T>

    @Composable
    fun DefaultRow(item: T) {

        var rowActive by state { false }

        Row(
            modifier = Modifier
                .background(color = if (rowActive) Color.LightGray else Color.White)
                .onPointerEvent(PointerEventType.Enter) { rowActive = true }
                .onPointerEvent(PointerEventType.Exit) { rowActive = false },
            verticalAlignment = Alignment.CenterVertically,
        ) {

            schema.columns.forEach { column ->
                Box(Modifier.weight(1F)) {
                    column.content(item)
                }
            }
        }
    }

    companion object {

        internal operator fun <T> invoke(schema: TableSchema<T>): TableRowScope<T> {
            return object : TableRowScope<T> {
                override val schema: TableSchema<T> = schema
            }
        }
    }
}

interface TableSchemaConfig<T> {

    fun addColumn(
        header: (@Composable () -> Unit)? = null,
        content: @Composable (T) -> Unit,
    )
}

internal class Column<T>(
    val header: (@Composable () -> Unit)? = null,
    val content: @Composable (T) -> Unit,
)

class TableSchema<T> : TableSchemaConfig<T> {

    internal val columns = mutableListOf<Column<T>>()

    override fun addColumn(
        header: (@Composable () -> Unit)?,
        content: @Composable (T) -> Unit,
    ) {
        columns.add(
            Column(
                header = header,
                content = content,
            )
        )
    }
}

fun <T> TableSchemaConfig<T>.addColumn(
    headerText: String,
    content: @Composable (T) -> Unit,
) {
    addColumn(
        header = { Text(headerText) },
        content = content,
    )
}

fun <T> TableSchemaConfig<T>.addColumnText(
    headerText: String,
    textSelector: (T) -> String,
) {
    addColumn(
        header = { Text(headerText) },
        content = { Text(textSelector(it)) },
    )
}

fun <T> tableSchema(block: TableSchemaConfig<T>.() -> Unit): TableSchema<T> = TableSchema<T>().apply { block() }

@Composable
fun <T> rememberTableSchema(block: TableSchemaConfig<T>.() -> Unit): TableSchema<T> = remember { tableSchema(block) }

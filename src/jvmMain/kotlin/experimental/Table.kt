package experimental

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Surface
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
import androidx.compose.ui.unit.dp
import utils.state

@Composable
internal fun <T> Table(
    items: List<T>,
    schema: TableSchema<T>,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    headerContent: @Composable TableHeaderScope<T>.() -> Unit = { DefaultHeader() },
    rowContent: @Composable TableRowScope<T>.(T) -> Unit = { item -> DefaultRow(item) },
) {

    val headerScope = remember(schema) { TableHeaderScope(schema) }
    val rowScope = remember(schema) { TableRowScope(schema) }

    LazyColumn(modifier = Modifier.padding(8.dp).then(modifier)) {

        stickyHeader {

            Surface {
                headerScope.headerContent()
            }

            Divider()
        }

        items(
            items = items,
            key = key,
            itemContent = { item -> rowScope.rowContent(item) },
        )
    }
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

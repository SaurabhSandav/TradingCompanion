package table

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

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

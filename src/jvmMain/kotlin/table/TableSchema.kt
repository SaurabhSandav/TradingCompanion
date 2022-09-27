package table

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

interface TableSchema<T> {

    val columns: List<Column<T>>

    fun addColumn(
        header: (@Composable () -> Unit)? = null,
        content: @Composable (T) -> Unit,
    )
}

internal class TableSchemaImpl<T> : TableSchema<T> {

    override val columns = mutableListOf<Column<T>>()

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

fun <T> TableSchema<T>.addColumn(
    headerText: String,
    content: @Composable (T) -> Unit,
) {
    addColumn(
        header = { Text(headerText) },
        content = content,
    )
}

fun <T> TableSchema<T>.addColumnText(
    headerText: String,
    contentText: (T) -> String,
) {
    addColumn(
        header = { Text(headerText) },
        content = { Text(contentText(it)) },
    )
}

fun <T> tableSchema(block: TableSchema<T>.() -> Unit): TableSchema<T> = TableSchemaImpl<T>().apply { block() }

@Composable
fun <T> rememberTableSchema(block: TableSchema<T>.() -> Unit): TableSchema<T> = remember { tableSchema(block) }

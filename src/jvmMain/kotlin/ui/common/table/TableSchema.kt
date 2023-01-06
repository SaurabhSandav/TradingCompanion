package ui.common.table

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

interface TableSchema<T> {

    val columns: List<Column<T>>

    fun addColumn(
        header: (@Composable () -> Unit)? = null,
        span: Float = 1F,
        content: @Composable (T) -> Unit,
    )
}

internal class TableSchemaImpl<T> : TableSchema<T> {

    override val columns = mutableListOf<Column<T>>()

    override fun addColumn(
        header: (@Composable () -> Unit)?,
        span: Float,
        content: @Composable (T) -> Unit,
    ) {
        columns.add(
            Column(
                header = header,
                span = span,
                content = content,
            )
        )
    }
}

fun <T> TableSchema<T>.addColumn(
    headerText: String,
    span: Float = 1F,
    content: @Composable (T) -> Unit,
) {
    addColumn(
        header = { Text(headerText) },
        span = span,
        content = content,
    )
}

fun <T> TableSchema<T>.addColumnText(
    headerText: String,
    span: Float = 1F,
    contentText: (T) -> String,
) {
    addColumn(
        header = { Text(headerText) },
        span = span,
        content = { Text(contentText(it)) },
    )
}

fun <T> tableSchema(block: TableSchema<T>.() -> Unit): TableSchema<T> = TableSchemaImpl<T>().apply { block() }

@Composable
fun <T> rememberTableSchema(block: TableSchema<T>.() -> Unit): TableSchema<T> = remember { tableSchema(block) }

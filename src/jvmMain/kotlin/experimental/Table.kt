import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    schema: TableSchema<T>,
) {

    LazyColumn(modifier = Modifier.padding(8.dp).then(modifier)) {

        stickyHeader {

            Surface {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    schema.columns.forEach { column ->
                        Box(Modifier.weight(1F)) {
                            Text(column.header)
                        }
                    }
                }
            }

            Divider()
        }

        items(
            items = items,
            key = key,
        ) { item ->

            Column {

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

                Divider()
            }
        }
    }
}

interface TableSchemaConfig<T> {

    fun addColumn(
        header: String,
        content: @Composable (T) -> Unit,
    )
}

internal class Column<T>(
    val header: String,
    val content: @Composable (T) -> Unit,
)

class TableSchema<T> : TableSchemaConfig<T> {

    internal val columns = mutableListOf<Column<T>>()

    override fun addColumn(
        header: String,
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

fun <T> TableSchemaConfig<T>.addColumnText(
    header: String,
    textSelector: (T) -> String,
) {
    addColumn(
        header = header,
        content = { Text(textSelector(it)) },
    )
}

fun <T> tableSchema(block: TableSchemaConfig<T>.() -> Unit): TableSchema<T> = TableSchema<T>().apply { block() }

fun <T> rememberTableSchema(block: TableSchemaConfig<T>.() -> Unit): TableSchema<T> = remember { tableSchema(block) }

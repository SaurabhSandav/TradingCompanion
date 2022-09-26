import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun <T> Table(
    items: List<T>,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    content: TableScope<T>.() -> Unit,
) {

    val tableScope = remember { TableScopeImpl<T>().apply { content() } }

    LazyColumn(modifier = modifier) {

        stickyHeader {

            Surface {

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    tableScope.columns.forEach { (header, _) ->
                        Box(Modifier.weight(1F)) {
                            header()
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

            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                tableScope.columns.forEach { (_, itemContent) ->
                    Box(Modifier.weight(1F)) {
                        itemContent(item)
                    }
                }
            }

            Divider()
        }
    }
}

interface TableScope<T> {

    fun column(
        header: @Composable BoxScope.() -> Unit,
        content: @Composable BoxScope.(T) -> Unit,
    )
}

private class TableScopeImpl<T> : TableScope<T> {

    val columns = mutableMapOf<@Composable BoxScope.() -> Unit, @Composable BoxScope.(T) -> Unit>()

    override fun column(
        header: @Composable BoxScope.() -> Unit,
        content: @Composable BoxScope.(T) -> Unit,
    ) {
        columns[header] = content
    }
}

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp

@Composable
internal fun <T> Table(
    items: List<T>,
    modifier: Modifier = Modifier,
    content: TableScope<T>.() -> Unit,
) {

    val tableScope = remember { TableScopeImpl<T>().apply { content() } }

    Row(modifier) {

        for ((header, itemContent) in tableScope.columns) {

            Column(Modifier.border(BorderStroke(1.dp, Color.Black))) {

                header()

                for (item in items) {
                    itemContent(item)
                }
            }
        }
    }

    Layout(
        modifier = modifier,
        content = {
            for ((header, itemContent) in tableScope.columns) {
                Column {

                    header()

                    for (item in items) {
                        itemContent(item)
                    }
                }
            }
        },
    ) { measurables, constraints ->

        layout(constraints.maxWidth, constraints.maxHeight) {

        }
    }
}

interface TableScope<T> {

    fun column(
        header: @Composable () -> Unit,
        content: @Composable (T) -> Unit,
    )
}

private class TableScopeImpl<T> : TableScope<T> {

    val columns = mutableMapOf<@Composable () -> Unit, @Composable (T) -> Unit>()

    override fun column(
        header: @Composable () -> Unit,
        content: @Composable (T) -> Unit,
    ) {
        columns[header] = content
    }
}

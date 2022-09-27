package table

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun <T> LazyTable(
    schema: TableSchema<T>,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = { DefaultTableHeader(schema) },
    content: TableScope<T>.() -> Unit,
) {

    Column(modifier = modifier) {

        headerContent()

        Divider()

        LazyColumn {
            TableScopeImpl(this, schema).content()
        }
    }
}

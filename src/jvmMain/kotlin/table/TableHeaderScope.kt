package table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun <T> DefaultTableHeader(
    schema: TableSchema<T>,
    modifier: Modifier = Modifier,
) {

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

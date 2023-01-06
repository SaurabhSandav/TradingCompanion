package ui.common.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import ui.common.state

@Composable
fun <T> DefaultTableHeader(
    schema: TableSchema<T>,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier.height(64.dp).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        schema.columns.forEach { column ->
            Box(Modifier.weight(column.span)) {
                column.header?.invoke()
            }
        }
    }
}

@Composable
fun <T> DefaultTableRow(
    item: T,
    schema: TableSchema<T>,
    modifier: Modifier = Modifier,
) {

    var rowActive by state { false }

    Surface(
        color = when {
            rowActive -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        }
    ) {

        Row(
            modifier = Modifier
                .onPointerEvent(PointerEventType.Enter) { rowActive = true }
                .onPointerEvent(PointerEventType.Exit) { rowActive = false }
                .padding(8.dp)
                .then(modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            schema.columns.forEach { column ->
                Box(Modifier.weight(column.span)) {
                    column.content(item)
                }
            }
        }
    }
}

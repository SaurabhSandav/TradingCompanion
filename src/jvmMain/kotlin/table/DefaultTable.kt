package table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import utils.state

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

@Composable
fun <T> DefaultTableRow(
    item: T,
    schema: TableSchema<T>,
    modifier: Modifier = Modifier,
) {

    var rowActive by state { false }

    Row(
        modifier = Modifier
            .background(color = if (rowActive) Color.LightGray else Color.White)
            .onPointerEvent(PointerEventType.Enter) { rowActive = true }
            .onPointerEvent(PointerEventType.Exit) { rowActive = false }
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        schema.columns.forEach { column ->
            Box(Modifier.weight(1F)) {
                column.content(item)
            }
        }
    }
}

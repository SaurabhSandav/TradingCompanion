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

interface TableRowScope<T> {

    val schema: TableSchema<T>

    @Composable
    fun DefaultRow(item: T) {

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
    }

    companion object {

        internal operator fun <T> invoke(schema: TableSchema<T>): TableRowScope<T> {
            return object : TableRowScope<T> {
                override val schema: TableSchema<T> = schema
            }
        }
    }
}

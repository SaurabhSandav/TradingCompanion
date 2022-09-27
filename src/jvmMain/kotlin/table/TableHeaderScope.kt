package table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

interface TableHeaderScope<T> {

    val schema: TableSchema<T>

    @Composable
    fun DefaultHeader(modifier: Modifier = Modifier) {

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


    companion object {
        internal operator fun <T> invoke(schema: TableSchema<T>): TableHeaderScope<T> {
            return object : TableHeaderScope<T> {
                override val schema: TableSchema<T> = schema
            }
        }
    }
}

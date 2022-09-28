package studies

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import ui.common.table.DefaultTableRow
import ui.common.table.LazyTable
import ui.common.table.TableSchema
import ui.common.table.rows

abstract class TableStudy<T> : Study {

    abstract val schema: TableSchema<T>

    abstract val data: Flow<List<T>>

    @Composable
    final override fun render() {

        val items by data.collectAsState(emptyList())

        LazyTable(
            modifier = Modifier.fillMaxSize(),
            schema = schema,
        ) {

            rows(
                items = items,
            ) { item ->

                Column {

                    DefaultTableRow(item, schema)

                    Divider()
                }
            }
        }
    }
}

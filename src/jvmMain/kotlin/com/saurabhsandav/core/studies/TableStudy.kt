package com.saurabhsandav.core.studies

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.table.DefaultTableRow
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.rows
import kotlinx.coroutines.flow.Flow

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

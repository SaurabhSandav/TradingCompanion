package com.saurabhsandav.core.ui.common.table

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.table.Column.Width.Weight

interface TableSchema<T> {

    val columns: List<Column<T>>

    fun addColumn(
        header: (@Composable () -> Unit)? = null,
        width: Column.Width = Weight(1F),
        content: @Composable (T) -> Unit,
    )
}

internal class TableSchemaImpl<T> : TableSchema<T> {

    override val columns = mutableListOf<Column<T>>()

    override fun addColumn(
        header: (@Composable () -> Unit)?,
        width: Column.Width,
        content: @Composable (T) -> Unit,
    ) {
        columns.add(
            Column(
                header = header,
                width = width,
                content = content,
            )
        )
    }
}

fun <T> TableSchema<T>.addColumn(
    headerText: String,
    width: Column.Width = Weight(1F),
    content: @Composable (T) -> Unit,
) {

    addColumn(
        header = {

            Text(
                text = headerText,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        width = width,
        content = content,
    )
}

fun <T> TableSchema<T>.addColumnText(
    headerText: String,
    width: Column.Width = Weight(1F),
    contentText: (T) -> String,
) {
    addColumn(
        header = {

            Text(
                text = headerText,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        width = width,
        content = {

            Text(
                text = contentText(it),
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

fun <T> tableSchema(block: TableSchema<T>.() -> Unit): TableSchema<T> = TableSchemaImpl<T>().apply { block() }

@Composable
fun <T> rememberTableSchema(block: TableSchema<T>.() -> Unit): TableSchema<T> = remember { tableSchema(block) }

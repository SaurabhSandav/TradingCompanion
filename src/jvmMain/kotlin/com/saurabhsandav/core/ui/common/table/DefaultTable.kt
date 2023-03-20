package com.saurabhsandav.core.ui.common.table

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    onClick: () -> Unit = {},
) {

    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
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

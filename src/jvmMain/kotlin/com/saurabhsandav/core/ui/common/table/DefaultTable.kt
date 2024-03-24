package com.saurabhsandav.core.ui.common.table

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.theme.dimens

@Composable
fun <T> DefaultTableHeader(
    schema: TableSchema<T>,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier.height(64.dp).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
    ) {

        schema.columns.forEach { column ->

            Box(column.width.asModifier()) {
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
    onLongClick: () -> Unit = {},
) {

    Row(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(8.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
    ) {

        schema.columns.forEach { column ->

            Box(column.width.asModifier()) {
                column.content(item)
            }
        }
    }
}

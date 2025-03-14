package com.saurabhsandav.core.ui.common.table

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.theme.dimens

context (_: ColumnScope)
@Composable
fun <T : TableSchema> T.SimpleHeader(
    modifier: Modifier = Modifier,
    builder: context(RowBuilder) T.() -> Unit,
) {

    Row(
        modifier = modifier.height(MaterialTheme.dimens.listHeaderHeight)
            .padding(MaterialTheme.dimens.listItemPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
    ) {

        val rowBuilder = remember(this@SimpleHeader, builder) {
            RowBuilderImpl().apply { builder(this@SimpleHeader) }
        }

        cells.forEach { cell ->

            val content = rowBuilder.contents[cell]

            Box(
                modifier = cell.width.asModifier(),
                contentAlignment = cell.contentAlignment,
                content = { content?.invoke() },
            )
        }
    }

    HorizontalDivider()
}

@Composable
fun <T : TableSchema> T.SimpleRow(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    builder: context(RowBuilder) T.() -> Unit,
) {

    Row(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(MaterialTheme.dimens.listItemPadding)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
    ) {

        val rowBuilder = remember(this@SimpleRow, builder) {
            RowBuilderImpl().apply { builder() }
        }

        cells.forEach { cell ->

            val content = rowBuilder.contents[cell]

            Box(
                modifier = cell.width.asModifier(),
                contentAlignment = cell.contentAlignment,
                content = { content?.invoke() },
            )
        }
    }
}

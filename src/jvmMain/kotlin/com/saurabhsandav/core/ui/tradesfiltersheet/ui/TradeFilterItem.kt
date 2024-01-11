package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state

@Composable
internal fun TradeFilterItem(
    title: String,
    expandInitially: Boolean,
    content: @Composable () -> Unit,
) {

    Column {

        var expanded by state { expandInitially }

        ListItem(
            modifier = Modifier.clickable { expanded = !expanded },
            headlineContent = { Text(title) },
            trailingContent = {

                IconButtonWithTooltip(
                    onClick = { expanded = !expanded },
                    tooltipText = if (expanded) "Close" else "Open",
                ) {

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Close" else "Open"
                    )
                }
            },
            shadowElevation = 8.dp,
        )

        AnimatedVisibility(expanded) {
            content()
        }
    }
}

@Composable
internal fun TradeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {

    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = selected.takeIf { it }?.let {
            {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Done",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        },
        label = { Text(label) }
    )
}

@Composable
internal fun TradeFilterChipGroup(
    title: String,
    expandInitially: Boolean,
    content: @Composable FlowRowScope.() -> Unit,
) {

    TradeFilterItem(
        title = title,
        expandInitially = expandInitially,
    ) {

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

package com.saurabhsandav.core.ui.common.controls

import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.theme.dimens

@Composable
fun ChipsSelectorBox(
    addButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selectedItems: @Composable () -> Unit,
) {

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.rowHorizontalSpacing,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.rowVerticalSpacing,
            alignment = Alignment.CenterVertically,
        ),
    ) {

        selectedItems()

        addButton()
    }
}

@Composable
fun ChipsSelectorSelectedItem(
    name: String,
    description: String? = null,
    onRemove: () -> Unit,
) {

    TooltipArea(
        tooltip = { if (description != null) Tooltip(description) },
    ) {

        InputChip(
            selected = false,
            onClick = onRemove,
            label = { Text(name) },
            trailingIcon = {

                Icon(
                    modifier = Modifier.size(InputChipDefaults.IconSize),
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                )
            },
        )
    }
}

@Composable
fun ChipsSelectorAddButton(
    onAdd: () -> Unit,
) {

    IconButtonWithTooltip(
        onClick = onAdd,
        tooltipText = "Add",
        content = {
            Icon(Icons.Default.Add, contentDescription = "Add")
        },
    )
}

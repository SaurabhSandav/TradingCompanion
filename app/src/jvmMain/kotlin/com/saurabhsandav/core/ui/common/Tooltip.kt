package com.saurabhsandav.core.ui.common

import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

// Tooltips from Google don't appear on hover.
// Prefer Jetbrains tooltips until multiplatform support.
@Composable
fun Tooltip(text: String) {

    Surface(
        modifier = Modifier.shadow(4.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface,
        shape = RoundedCornerShape(4.dp),
    ) {

        Text(
            text = text,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun IconButtonWithTooltip(
    onClick: () -> Unit,
    tooltipText: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {

    TooltipArea(
        tooltip = {

            // Override custom text styles
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                Tooltip(tooltipText)
            }
        },
        content = {
            IconButton(onClick, modifier, enabled, colors, interactionSource, content)
        },
    )
}

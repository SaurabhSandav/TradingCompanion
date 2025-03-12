package com.saurabhsandav.core.ui.common

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun SimpleTooltipBox(
    tooltipText: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {

    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            if (tooltipText != null) PlainTooltip { Text(tooltipText) }
        },
        state = rememberTooltipState(),
        content = content,
    )
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

    SimpleTooltipBox(tooltipText) {
        IconButton(onClick, modifier, enabled, colors, interactionSource, content)
    }
}

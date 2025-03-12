package com.saurabhsandav.core.ui.common.chart.legend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state

@Composable
fun Legend(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {

    Column(modifier, content = content)
}

@Composable
fun LegendItem(
    label: @Composable () -> Unit,
    controls: (@Composable RowScope.() -> Unit)? = null,
    values: @Composable RowScope.() -> Unit,
) {

    ProvideTextStyle(MaterialTheme.typography.labelLarge) {

        Box {

            var isHovered by state { false }

            Row(
                modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Box(
                    modifier = Modifier
                        .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                        .onPointerEvent(PointerEventType.Exit) { isHovered = false },
                    propagateMinConstraints = true,
                ) {
                    label()
                }

                values()
            }

            if (controls != null && isHovered) {

                Row(
                    modifier = Modifier
                        .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                        .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small)
                        .padding(vertical = 2.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    label()

                    controls()
                }
            }
        }
    }
}

@Composable
fun LegendVisibilityButton(
    isEnabled: Boolean,
    onToggleIsEnabled: () -> Unit,
) {

    val tooltipText = if (isEnabled) "Hide" else "Show"

    IconButtonWithTooltip(
        modifier = Modifier.height(16.dp),
        onClick = onToggleIsEnabled,
        tooltipText = tooltipText,
        content = {

            Icon(
                imageVector = when {
                    isEnabled -> Icons.Default.Visibility
                    else -> Icons.Default.VisibilityOff
                },
                contentDescription = tooltipText,
            )
        },
    )
}

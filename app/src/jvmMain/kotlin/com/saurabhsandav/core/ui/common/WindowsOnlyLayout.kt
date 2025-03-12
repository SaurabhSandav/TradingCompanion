package com.saurabhsandav.core.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout

/**
 * Shows only separate windows. Does not place any children in current window.
 */
@Composable
fun WindowsOnlyLayout(content: @Composable () -> Unit) {

    Layout(
        content = content,
        measurePolicy = { measurables, _ ->

            check(measurables.isEmpty()) { "WindowsOnlyLayout: Non window content emitted" }

            layout(0, 0) {}
        },
    )
}

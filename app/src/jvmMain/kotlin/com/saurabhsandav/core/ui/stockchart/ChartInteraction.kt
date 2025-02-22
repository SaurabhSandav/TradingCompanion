package com.saurabhsandav.core.ui.stockchart

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType

internal class ChartInteraction(
    private val onChartHover: () -> Unit,
    private val onChartSelected: () -> Unit,
) {

    var size: Size = Size.Zero

    fun onEvent(event: PointerEvent) {

        val callback = when (event.type) {
            PointerEventType.Enter, PointerEventType.Move -> onChartHover
            PointerEventType.Press -> onChartSelected
            else -> return
        }

        callback()
    }
}

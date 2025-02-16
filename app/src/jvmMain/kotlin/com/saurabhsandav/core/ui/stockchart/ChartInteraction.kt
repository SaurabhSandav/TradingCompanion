package com.saurabhsandav.core.ui.stockchart

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import com.saurabhsandav.core.ui.stockchart.ui.ChartsLayout
import com.saurabhsandav.core.ui.stockchart.ui.getComposeRects

internal class ChartInteraction(
    private val layout: () -> ChartsLayout,
    private val onChartHover: (Int) -> Unit,
    private val onChartSelected: (Int) -> Unit,
) {

    var size: Size = Size.Zero

    fun onEvent(event: PointerEvent) {

        val callback = when (event.type) {
            PointerEventType.Enter, PointerEventType.Move -> onChartHover
            PointerEventType.Press -> onChartSelected
            else -> return
        }

        val layout = layout()
        val chartIndex = getChartIndex(event, layout) ?: return

        callback(chartIndex)
    }

    private fun getChartIndex(
        event: PointerEvent,
        layout: ChartsLayout,
    ): Int? {

        val position = event.changes.first().position
        val rects = layout.getComposeRects(size)
        val rect = rects.find { rect -> rect.contains(position) } ?: return null

        return rects.indexOf(rect)
    }
}

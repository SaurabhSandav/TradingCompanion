package com.saurabhsandav.core.chart.data

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.IsJsonElement
import com.saurabhsandav.core.ui.common.toHexString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class SeriesMarker(
    val time: Time,
    val position: SeriesMarkerPosition,
    val shape: SeriesMarkerShape,
    val color: Color,
    val id: String? = null,
    val text: String? = null,
    val size: Number? = null,
) : IsJsonElement {

    override fun toJsonElement(): JsonElement = buildJsonObject {
        put("time", time.toJsonElement())
        put("position", position.strValue)
        put("shape", shape.strValue)
        put("color", color.toHexString())
        id?.let { put("id", it) }
        text?.let { put("text", it) }
        size?.let { put("size", it) }
    }
}

enum class SeriesMarkerPosition(val strValue: String) {
    AboveBar("aboveBar"),
    BelowBar("belowBar"),
    InBar("inBar");
}

enum class SeriesMarkerShape(val strValue: String) {
    Circle("circle"),
    Square("square"),
    ArrowUp("arrowUp"),
    ArrowDown("arrowDown");
}

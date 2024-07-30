package com.saurabhsandav.lightweight_charts.options

import com.saurabhsandav.lightweight_charts.IsJsonElement
import com.saurabhsandav.lightweight_charts.options.common.LineStyle
import com.saurabhsandav.lightweight_charts.options.common.LineWidth
import kotlinx.css.Color
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class PriceLineOptions(
    val id: String? = null,
    val price: Double? = null,
    val color: Color? = null,
    val lineWidth: LineWidth? = null,
    val lineStyle: LineStyle? = null,
    val lineVisible: Boolean? = null,
    val axisLabelVisible: Boolean? = null,
    val title: String? = null,
    val axisLabelColor: Color? = null,
    val axisLabelTextColor: Color? = null,
) : IsJsonElement {

    override fun toJsonElement(): JsonObject = buildJsonObject {
        id?.let { put("id", it) }
        price?.let { put("price", it) }
        color?.let { put("color", it.value) }
        lineWidth?.let { put("lineWidth", it.toJsonElement()) }
        lineStyle?.let { put("lineStyle", it.toJsonElement()) }
        lineVisible?.let { put("lineVisible", it) }
        axisLabelVisible?.let { put("axisLabelVisible", it) }
        title?.let { put("title", it) }
        axisLabelColor?.let { put("axisLabelColor", it.value) }
        axisLabelTextColor?.let { put("axisLabelTextColor", it.value) }
    }
}

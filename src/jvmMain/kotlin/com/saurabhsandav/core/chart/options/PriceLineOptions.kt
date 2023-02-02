package com.saurabhsandav.core.chart.options

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.IsJsonElement
import com.saurabhsandav.core.chart.options.common.LineStyle
import com.saurabhsandav.core.chart.options.common.LineWidth
import com.saurabhsandav.core.ui.common.toHexString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class PriceLineOptions(
    val price: Number? = null,
    val color: Color? = null,
    val lineWidth: LineWidth? = null,
    val lineStyle: LineStyle? = null,
    val lineVisible: Boolean? = null,
    val axisLabelVisible: Boolean? = null,
    val title: String? = null,
) : IsJsonElement {

    override fun toJsonElement(): JsonObject = buildJsonObject {
        price?.let { put("price", it) }
        color?.let { put("color", it.toHexString()) }
        lineWidth?.let { put("lineWidth", it.toJsonElement()) }
        lineStyle?.let { put("lineStyle", it.toJsonElement()) }
        lineVisible?.let { put("lineVisible", it) }
        axisLabelVisible?.let { put("axisLabelVisible", it) }
        title?.let { put("title", it) }
    }
}

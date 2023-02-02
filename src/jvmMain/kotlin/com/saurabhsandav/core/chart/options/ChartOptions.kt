package com.saurabhsandav.core.chart.options

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.IsJsonElement
import com.saurabhsandav.core.chart.PriceScaleOptions
import com.saurabhsandav.core.chart.options.common.Background
import com.saurabhsandav.core.chart.options.common.LineStyle
import com.saurabhsandav.core.ui.common.toHexString
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class ChartOptions(
    val width: Number? = null,
    val height: Number? = null,
    val layout: LayoutOptions? = null,
    val rightPriceScale: PriceScaleOptions? = null,
    val timeScale: TimeScaleOptions? = null,
    val crosshair: CrosshairOptions? = null,
    val grid: GridOptions? = null,
) : IsJsonElement {

    override fun toJsonElement() = buildJsonObject {
        width?.let { put("width", it) }
        height?.let { put("height", it) }
        layout?.let { put("layout", it.toJsonElement()) }
        rightPriceScale?.let { put("rightPriceScale", it.toJsonElement()) }
        timeScale?.let { put("timeScale", it.toJsonElement()) }
        crosshair?.let { put("crosshair", it.toJsonElement()) }
        grid?.let { put("grid", it.toJsonElement()) }
    }
}

data class LayoutOptions(
    val background: Background? = null,
    val textColor: Color? = null,
) : IsJsonElement {

    override fun toJsonElement() = buildJsonObject {
        background?.let { put("background", it.toJsonElement()) }
        textColor?.let { put("textColor", it.toHexString()) }
    }
}

data class CrosshairOptions(
    val mode: CrosshairMode? = null,
) : IsJsonElement {

    override fun toJsonElement() = buildJsonObject {
        mode?.let { put("mode", it.toJsonElement()) }
    }
}

data class GridOptions(
    val vertLines: GridLineOptions? = null,
    val horzLines: GridLineOptions? = null,
) : IsJsonElement {

    override fun toJsonElement() = buildJsonObject {
        vertLines?.let { put("vertLines", it.toJsonElement()) }
        horzLines?.let { put("horzLines", it.toJsonElement()) }
    }
}

data class GridLineOptions(
    val color: Color? = null,
    val style: LineStyle? = null,
    val visible: Boolean? = null,
) : IsJsonElement {

    override fun toJsonElement() = buildJsonObject {
        color?.let { put("color", it.toHexString()) }
        style?.let { put("style", it.toJsonElement()) }
        visible?.let { put("visible", it) }
    }
}

enum class CrosshairMode(private val intValue: Int) : IsJsonElement {
    Normal(0),
    Magnet(1);

    override fun toJsonElement() = JsonPrimitive(intValue)
}

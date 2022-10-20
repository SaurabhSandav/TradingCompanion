package chart

import androidx.compose.ui.graphics.Color
import chart.misc.Background
import chart.misc.LineStyle
import chart.pricescale.PriceScaleOptions
import chart.timescale.TimeScaleOptions
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ui.common.toHexString

data class ChartOptions(
    val width: Number? = null,
    val height: Number? = null,
    val layout: LayoutOptions? = null,
    val rightPriceScale: PriceScaleOptions? = null,
    val timeScale: TimeScaleOptions? = null,
    val crosshair: CrosshairOptions? = null,
    val grid: GridOptions? = null,
) {

    fun toJsonObject() = buildJsonObject {
        width?.let { put("width", it) }
        height?.let { put("height", it) }
        layout?.let { put("layout", it.toJsonObject()) }
        rightPriceScale?.let { put("rightPriceScale", it.toJsonObject()) }
        timeScale?.let { put("timeScale", it.toJsonObject()) }
        crosshair?.let { put("crosshair", it.toJsonObject()) }
        grid?.let { put("grid", it.toJsonObject()) }
    }
}

data class LayoutOptions(
    val background: Background? = null,
    val textColor: Color? = null,
) {

    fun toJsonObject() = buildJsonObject {
        background?.let { put("background", it.toJsonObject()) }
        textColor?.let { put("textColor", it.toHexString()) }
    }
}

data class CrosshairOptions(
    val mode: CrosshairMode? = null,
) {

    fun toJsonObject() = buildJsonObject {
        mode?.let { put("mode", it.intValue) }
    }
}

data class GridOptions(
    val vertLines: GridLineOptions? = null,
    val horzLines: GridLineOptions? = null,
) {

    fun toJsonObject() = buildJsonObject {
        vertLines?.let { put("vertLines", it.toJsonObject()) }
        horzLines?.let { put("horzLines", it.toJsonObject()) }
    }
}

data class GridLineOptions(
    val color: Color? = null,
    val style: LineStyle? = null,
    val visible: Boolean? = null,
) {

    fun toJsonObject() = buildJsonObject {
        color?.let { put("color", it.toHexString()) }
        style?.let { put("style", it.strValue) }
        visible?.let { put("visible", it) }
    }
}

enum class CrosshairMode(val intValue: Int) {
    Normal(0),
    Magnet(1);
}

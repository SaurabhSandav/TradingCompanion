package com.saurabhsandav.lightweight_charts.options

import com.saurabhsandav.lightweight_charts.IsJsonElement
import com.saurabhsandav.lightweight_charts.PriceScaleOptions
import com.saurabhsandav.lightweight_charts.options.common.Background
import com.saurabhsandav.lightweight_charts.options.common.LineStyle
import com.saurabhsandav.lightweight_charts.options.common.LineWidth
import kotlinx.css.Color
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class ChartOptions(
    val width: Double? = null,
    val height: Double? = null,
    val autoSize: Boolean? = null,
    val layout: LayoutOptions? = null,
    val leftPriceScale: PriceScaleOptions? = null,
    val rightPriceScale: PriceScaleOptions? = null,
    val timeScale: TimeScaleOptions? = null,
    val crosshair: CrosshairOptions? = null,
    val grid: GridOptions? = null,
) : IsJsonElement {

    override fun toJsonElement() = buildJsonObject {
        width?.let { put("width", it) }
        height?.let { put("height", it) }
        autoSize?.let { put("autoSize", it) }
        layout?.let { put("layout", it.toJsonElement()) }
        leftPriceScale?.let { put("leftPriceScale", it.toJsonElement()) }
        rightPriceScale?.let { put("rightPriceScale", it.toJsonElement()) }
        timeScale?.let { put("timeScale", it.toJsonElement()) }
        crosshair?.let { put("crosshair", it.toJsonElement()) }
        grid?.let { put("grid", it.toJsonElement()) }
    }

    data class LayoutOptions(
        val background: Background? = null,
        val textColor: Color? = null,
    ) : IsJsonElement {

        override fun toJsonElement() = buildJsonObject {
            background?.let { put("background", it.toJsonElement()) }
            textColor?.let { put("textColor", it.value) }
        }
    }

    data class CrosshairOptions(
        val mode: CrosshairMode? = null,
        val vertLine: CrosshairLineOptions? = null,
        val horzLine: CrosshairLineOptions? = null,
    ) : IsJsonElement {

        override fun toJsonElement() = buildJsonObject {
            mode?.let { put("mode", it.toJsonElement()) }
            vertLine?.let { put("vertLine", it.toJsonElement()) }
            horzLine?.let { put("horzLine", it.toJsonElement()) }
        }

        enum class CrosshairMode(private val intValue: Int) : IsJsonElement {
            Normal(0),
            Magnet(1),
            Hidden(2);

            override fun toJsonElement() = JsonPrimitive(intValue)
        }

        data class CrosshairLineOptions(
            val color: Color? = null,
            val width: LineWidth? = null,
            val style: LineStyle? = null,
            val visible: Boolean? = null,
            val labelVisible: Boolean? = null,
            val labelBackgroundColor: Color? = null,
        ) : IsJsonElement {

            override fun toJsonElement() = buildJsonObject {

                color?.let { put("color", it.value) }
                style?.let { put("style", it.toJsonElement()) }
                width?.let { put("width", it.toJsonElement()) }
                visible?.let { put("visible", it) }
                labelVisible?.let { put("labelVisible", it) }
                labelBackgroundColor?.let { put("labelBackgroundColor", it.value) }
            }
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

        data class GridLineOptions(
            val color: Color? = null,
            val style: LineStyle? = null,
            val visible: Boolean? = null,
        ) : IsJsonElement {

            override fun toJsonElement() = buildJsonObject {
                color?.let { put("color", it.value) }
                style?.let { put("style", it.toJsonElement()) }
                visible?.let { put("visible", it) }
            }
        }
    }
}

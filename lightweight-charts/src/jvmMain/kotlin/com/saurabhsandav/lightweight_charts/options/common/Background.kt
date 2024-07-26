package com.saurabhsandav.lightweight_charts.options.common

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.lightweight_charts.IsJsonElement
import com.saurabhsandav.lightweight_charts.toHexString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed class Background : IsJsonElement {

    data class SolidColor(
        val color: Color,
    ) : Background() {

        override fun toJsonElement(): JsonElement = buildJsonObject {
            put("type", "ColorType.Solid")
            put("color", color.toHexString())
        }
    }

    data class VerticalGradientColor(
        val topColor: Color? = null,
        val bottomColor: Color? = null,
    ) : Background() {

        override fun toJsonElement() = buildJsonObject {
            put("type", "ColorType.VerticalGradient")
            topColor?.let { put("topColor", it.toHexString()) }
            bottomColor?.let { put("bottomColor", it.toHexString()) }
        }
    }
}

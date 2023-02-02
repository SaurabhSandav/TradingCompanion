package com.saurabhsandav.core.chart.options.common

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.IsJsonElement
import com.saurabhsandav.core.ui.common.toHexString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed class Background : IsJsonElement {

    data class SolidColor(
        val color: Color,
    ) : Background() {

        override fun toJsonElement(): JsonElement = buildJsonObject {
            put("type", "Solid")
            put("color", color.toHexString())
        }
    }

    data class VerticalGradientColor(
        val topColor: Color? = null,
        val bottomColor: Color? = null,
    ) : Background() {

        override fun toJsonElement() = buildJsonObject {
            put("type", "Solid")
            topColor?.let { put("topColor", it.toHexString()) }
            bottomColor?.let { put("bottomColor", it.toHexString()) }
        }
    }
}

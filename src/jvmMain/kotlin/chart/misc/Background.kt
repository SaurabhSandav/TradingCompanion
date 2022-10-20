package chart.misc

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ui.common.toHexString

sealed class Background {

    abstract fun toJsonObject(): JsonElement

    data class SolidColor(
        val color: Color,
    ) : Background() {

        override fun toJsonObject() = buildJsonObject {
            put("type", "Solid")
            put("color", color.toHexString())
        }
    }

    data class VerticalGradientColor(
        val topColor: Color? = null,
        val bottomColor: Color? = null,
    ) : Background() {

        override fun toJsonObject() = buildJsonObject {
            put("type", "Solid")
            topColor?.let { put("topColor", it.toHexString()) }
            bottomColor?.let { put("bottomColor", it.toHexString()) }
        }
    }
}

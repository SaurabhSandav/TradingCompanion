package com.saurabhsandav.lightweight_charts.options.common

import com.saurabhsandav.lightweight_charts.IsJsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed class PriceFormat : IsJsonElement {

    abstract val minMove: Number?

    data class BuiltIn(
        val type: Type? = null,
        val precision: Number? = null,
        override val minMove: Number? = null,
    ) : PriceFormat() {

        override fun toJsonElement(): JsonObject = buildJsonObject {
            type?.let { put("type", it.toJsonElement()) }
            precision?.let { put("precision", precision) }
            minMove?.let { put("minMove", minMove) }
        }
    }

    data class Custom(
        override val minMove: Number? = null,
    ) : PriceFormat() {

        override fun toJsonElement(): JsonObject = buildJsonObject {
            put("type", "custom")
            minMove?.let { put("minMove", minMove) }
        }
    }

    enum class Type(private val strValue: String) : IsJsonElement {
        Percent("percent"),
        Price("price"),
        Volume("volume");

        override fun toJsonElement() = JsonPrimitive(strValue)
    }
}

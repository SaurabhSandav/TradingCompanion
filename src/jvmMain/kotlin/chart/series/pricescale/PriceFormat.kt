package chart.series.pricescale

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed class PriceFormat {

    abstract val minMove: Number?

    abstract fun toJsonObject(): JsonObject

    data class BuiltIn(
        val type: Type? = null,
        val precision: Number? = null,
        override val minMove: Number? = null,
    ) : PriceFormat() {

        override fun toJsonObject(): JsonObject = buildJsonObject {
            type?.let { put("type", it.strValue) }
            precision?.let { put("precision", precision) }
            minMove?.let { put("minMove", minMove) }
        }
    }

    data class Custom(
        override val minMove: Number? = null,
    ) : PriceFormat() {

        override fun toJsonObject(): JsonObject = buildJsonObject {
            put("type", "custom")
            minMove?.let { put("minMove", minMove) }
        }
    }

    enum class Type(val strValue: String) {
        Percent("percent"),
        Price("price"),
        Volume("volume"),
    }
}

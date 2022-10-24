package chart.options.common

import chart.IsJsonElement
import kotlinx.serialization.json.JsonPrimitive

enum class PriceLineSource(private val strValue: String) : IsJsonElement {
    LastBar("LastBar"),
    LastVisible("LastVisible");

    override fun toJsonElement() = JsonPrimitive(strValue)
}

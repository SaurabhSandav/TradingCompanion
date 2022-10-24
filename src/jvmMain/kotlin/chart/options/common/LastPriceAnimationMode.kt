package chart.options.common

import chart.IsJsonElement
import kotlinx.serialization.json.JsonPrimitive

enum class LastPriceAnimationMode(private val strValue: String) : IsJsonElement {
    Disabled("Disabled"),
    Continuous("Continuous"),
    OnDataUpdate("OnDataUpdateUpdate");

    override fun toJsonElement() = JsonPrimitive(strValue)
}

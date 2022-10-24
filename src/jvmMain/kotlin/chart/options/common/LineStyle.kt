package chart.options.common

import chart.IsJsonElement
import kotlinx.serialization.json.JsonPrimitive

enum class LineStyle(private val intValue: Int) : IsJsonElement {
    Solid(0),
    Dotted(1),
    Dashed(2),
    LargeDashed(3),
    SparseDotted(4);

    override fun toJsonElement() = JsonPrimitive(intValue)
}

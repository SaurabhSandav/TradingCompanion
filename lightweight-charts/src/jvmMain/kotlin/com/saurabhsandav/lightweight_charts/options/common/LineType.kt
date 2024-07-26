package com.saurabhsandav.lightweight_charts.options.common

import com.saurabhsandav.lightweight_charts.IsJsonElement
import kotlinx.serialization.json.JsonPrimitive

enum class LineType(private val intValue: Int) : IsJsonElement {
    Simple(0),
    WithSteps(1),
    Curved(2);

    override fun toJsonElement() = JsonPrimitive(intValue)
}

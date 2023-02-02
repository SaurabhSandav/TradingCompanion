package com.saurabhsandav.core.chart.options.common

import com.saurabhsandav.core.chart.IsJsonElement
import kotlinx.serialization.json.JsonPrimitive

enum class LineType(private val intValue: Int) : IsJsonElement {
    Simple(0),
    WithSteps(1);

    override fun toJsonElement() = JsonPrimitive(intValue)
}

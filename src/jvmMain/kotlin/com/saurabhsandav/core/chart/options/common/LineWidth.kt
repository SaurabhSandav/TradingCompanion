package com.saurabhsandav.core.chart.options.common

import com.saurabhsandav.core.chart.IsJsonElement
import kotlinx.serialization.json.JsonPrimitive

enum class LineWidth(private val intValue: Int) : IsJsonElement {
    One(1),
    Two(2),
    Three(3),
    Four(4);

    override fun toJsonElement() = JsonPrimitive(intValue)
}

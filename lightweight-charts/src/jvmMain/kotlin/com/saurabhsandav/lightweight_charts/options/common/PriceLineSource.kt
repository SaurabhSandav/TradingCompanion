package com.saurabhsandav.lightweight_charts.options.common

import com.saurabhsandav.lightweight_charts.IsJsonElement
import kotlinx.serialization.json.JsonPrimitive

enum class PriceLineSource(private val intValue: Int) : IsJsonElement {
    LastBar(0),
    LastVisible(1);

    override fun toJsonElement() = JsonPrimitive(intValue)
}

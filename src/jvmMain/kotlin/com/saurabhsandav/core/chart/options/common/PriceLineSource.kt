package com.saurabhsandav.core.chart.options.common

import com.saurabhsandav.core.chart.IsJsonElement
import kotlinx.serialization.json.JsonPrimitive

enum class PriceLineSource(private val intValue: Int) : IsJsonElement {
    LastBar(0),
    LastVisible(1);

    override fun toJsonElement() = JsonPrimitive(intValue)
}

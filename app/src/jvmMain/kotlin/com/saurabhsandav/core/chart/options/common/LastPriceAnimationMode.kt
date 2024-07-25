package com.saurabhsandav.core.chart.options.common

import com.saurabhsandav.core.chart.IsJsonElement
import kotlinx.serialization.json.JsonPrimitive

enum class LastPriceAnimationMode(private val intValue: Int) : IsJsonElement {
    Disabled(0),
    Continuous(1),
    OnDataUpdate(2);

    override fun toJsonElement() = JsonPrimitive(intValue)
}

package com.saurabhsandav.core.chart

import kotlinx.serialization.json.JsonElement

interface IsJsonElement {

    fun toJsonElement(): JsonElement
}

package com.saurabhsandav.lightweight_charts

import kotlinx.serialization.json.JsonElement

interface IsJsonElement {

    fun toJsonElement(): JsonElement
}

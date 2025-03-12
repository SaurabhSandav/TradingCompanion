package com.saurabhsandav.lightweight_charts.callbacks

import com.saurabhsandav.lightweight_charts.data.Time
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

internal fun timeFromCallbackJson(time: JsonElement?): Time? = when (time) {
    is JsonObject -> Time.BusinessDay(
        year = time["year"]!!.jsonPrimitive.int,
        month = time["month"]!!.jsonPrimitive.int,
        day = time["day"]!!.jsonPrimitive.int,
    )
    is JsonPrimitive if time.isString -> Time.ISOString(time.content)
    is JsonPrimitive -> Time.UTCTimestamp(time.long)
    else -> null
}

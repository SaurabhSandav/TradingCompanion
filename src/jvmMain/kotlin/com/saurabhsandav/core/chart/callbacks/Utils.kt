package com.saurabhsandav.core.chart.callbacks

import com.saurabhsandav.core.chart.data.Time
import kotlinx.serialization.json.*

internal fun timeFromCallbackJson(time: JsonElement?): Time? = when (time) {
    is JsonObject -> Time.BusinessDay(
        year = time["year"]!!.jsonPrimitive.int,
        month = time["month"]!!.jsonPrimitive.int,
        day = time["day"]!!.jsonPrimitive.int,
    )

    is JsonPrimitive -> when {
        time.isString -> Time.String(time.content)
        else -> Time.UTCTimestamp(time.long)
    }

    else -> null
}

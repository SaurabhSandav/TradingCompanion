package com.saurabhsandav.lightweight_charts.data

import com.saurabhsandav.lightweight_charts.callbacks.timeFromCallbackJson
import kotlinx.serialization.json.*

data class BarsInfo(
    val barsBefore: Float,
    val barsAfter: Float,
    val from: Time?,
    val to: Time?,
) {

    companion object {

        fun fromJson(jsonStr: String): BarsInfo? {

            val jsonElement = Json.parseToJsonElement(jsonStr)

            if (jsonElement == JsonNull) return null

            val barsBefore = jsonElement.jsonObject["barsBefore"]!!.jsonPrimitive.float
            val barsAfter = jsonElement.jsonObject["barsAfter"]!!.jsonPrimitive.float
            val from = timeFromCallbackJson(jsonElement.jsonObject["from"])
            val to = timeFromCallbackJson(jsonElement.jsonObject["to"])

            return BarsInfo(
                barsBefore = barsBefore,
                barsAfter = barsAfter,
                from = from,
                to = to,
            )
        }
    }
}

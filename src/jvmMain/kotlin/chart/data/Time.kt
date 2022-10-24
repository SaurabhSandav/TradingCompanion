package chart.data

import chart.IsJsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed class Time : IsJsonElement {

    data class UTCTimestamp(val value: Long) : Time() {

        override fun toJsonElement() = JsonPrimitive(value)
    }

    data class BusinessDay(val year: Int, val month: Int, val day: Int) : Time() {

        override fun toJsonElement() = buildJsonObject {
            put("year", year)
            put("month", month)
            put("day", day)
        }
    }

    data class String(val value: kotlin.String) : Time() {

        override fun toJsonElement() = JsonPrimitive(value)
    }
}

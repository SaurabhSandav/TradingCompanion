package chart.series

import kotlinx.serialization.json.*

interface ChartData

open class SingleValueData(
    val time: Time,
    val value: Number,
) : ChartData {

    open fun toJsonObject(): JsonObject = buildJsonObject {
        putSingleValueDataElements(this@SingleValueData)
    }

    companion object {

        internal fun JsonObjectBuilder.putSingleValueDataElements(data: SingleValueData) = with(data) {
            put("time", time.toJsonElement())
            put("value", value)
        }
    }
}

sealed class Time {

    abstract fun toJsonElement(): JsonElement

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

package chart.series.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

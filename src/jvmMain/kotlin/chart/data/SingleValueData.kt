package chart.data

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

open class SingleValueData(
    val time: Time,
    val value: Number,
) : SeriesData {

    override fun toJsonElement(): JsonElement = buildJsonObject {
        putSingleValueDataElements(this@SingleValueData)
    }

    protected fun JsonObjectBuilder.putSingleValueDataElements(data: SingleValueData) = with(data) {
        put("time", time.toJsonElement())
        put("value", value)
    }
}

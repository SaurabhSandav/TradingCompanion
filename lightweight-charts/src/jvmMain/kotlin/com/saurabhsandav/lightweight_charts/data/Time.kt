package com.saurabhsandav.lightweight_charts.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = TimeTransformingSerializer::class)
sealed class Time {

    @Serializable
    data class UTCTimestamp(
        val value: Long,
    ) : Time()

    @Serializable
    data class BusinessDay(
        val year: Int,
        val month: Int,
        val day: Int,
    ) : Time()

    @Serializable
    data class ISOString(
        val value: String,
    ) : Time()
}

private object TimeTransformingSerializer : JsonTransformingSerializer<Time>(TimeSerializer) {

    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonPrimitive -> buildJsonObject { put("value", element) }
            else -> element
        }
    }

    override fun transformSerialize(element: JsonElement): JsonElement {

        require(element is JsonObject)

        return element["value"]?.jsonPrimitive ?: element
    }
}

private object TimeSerializer : JsonContentPolymorphicSerializer<Time>(Time::class) {

    override fun selectDeserializer(element: JsonElement): KSerializer<out Time> {

        val value = (element as? JsonObject)?.get("value")?.let { it as? JsonPrimitive }

        return when {
            value != null -> if (value.isString) Time.ISOString.serializer() else Time.UTCTimestamp.serializer()
            else -> Time.BusinessDay.serializer()
        }
    }
}

package com.saurabhsandav.lightweight_charts.options.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.contentOrNull

@Serializable(with = CustomTransformingSerializer::class)
sealed class PriceFormat {

    abstract val minMove: Double?

    @Serializable
    data class BuiltIn(
        val type: Type,
        val precision: Double? = null,
        override val minMove: Double? = null,
    ) : PriceFormat()

    @Serializable
    data class Custom(
        override val minMove: Double?,
    ) : PriceFormat()

    @Serializable
    enum class Type {

        @SerialName("percent")
        Percent,

        @SerialName("price")
        Price,

        @SerialName("volume")
        Volume,
    }
}

private object CustomTransformingSerializer : JsonTransformingSerializer<PriceFormat>(PriceFormatSerializer) {

    override fun transformSerialize(element: JsonElement): JsonElement {

        require(element is JsonObject)

        val type = (element as? JsonObject)
            ?.get("type")
            ?.let { it as? JsonPrimitive }
            ?.takeIf { it.isString }
            ?.contentOrNull

        return if (type == null) {
            val typePair = "type" to JsonPrimitive("custom")
            JsonObject(element + typePair)
        } else {
            element
        }
    }
}

private object PriceFormatSerializer : JsonContentPolymorphicSerializer<PriceFormat>(PriceFormat::class) {

    override fun selectDeserializer(element: JsonElement): KSerializer<out PriceFormat> {

        val type = (element as? JsonObject)
            ?.get("type")
            ?.let { it as? JsonPrimitive }
            ?.takeIf { it.isString }
            ?.contentOrNull

        return when {
            type == "custom" -> PriceFormat.Custom.serializer()
            else -> PriceFormat.BuiltIn.serializer()
        }
    }
}

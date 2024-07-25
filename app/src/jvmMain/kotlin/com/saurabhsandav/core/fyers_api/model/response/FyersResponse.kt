package com.saurabhsandav.core.fyers_api.model.response

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable(with = FyersSerializer::class)
data class FyersResponse<T>(
    val s: String?,
    val code: Int?,
    val message: String?,
    val result: T?,
)

val FyersResponse<*>.isAuthError: Boolean
    get() = code == -8

internal class FyersSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<FyersResponse<T>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FyersResponse") {
        element("s", serialDescriptor<String>().nullable)
        element("code", serialDescriptor<Int>().nullable)
        element("message", serialDescriptor<String>().nullable)
        element("result", dataSerializer.descriptor.nullable)
    }

    override fun serialize(encoder: Encoder, value: FyersResponse<T>) = error("Not Supported")

    override fun deserialize(decoder: Decoder): FyersResponse<T> {
        // Decoder -> JsonDecoder
        require(decoder is JsonDecoder) // this class can be decoded only by Json
        // JsonDecoder -> JsonElement
        val jsonObject = decoder.decodeJsonElement().jsonObject

        val resultMap = jsonObject.filterKeys { key -> key !in FyersResponseCommonKeys }

        return FyersResponse(
            s = jsonObject["s"]?.jsonPrimitive?.content,
            code = jsonObject["code"]?.jsonPrimitive?.intOrNull,
            message = jsonObject["message"]?.jsonPrimitive?.content,
            result = when {
                resultMap.isEmpty() -> null
                else -> decoder.json.decodeFromJsonElement(
                    deserializer = dataSerializer,
                    element = JsonObject(resultMap),
                )
            },
        )
    }
}

private val FyersResponseCommonKeys = listOf("s", "code", "message")

package com.saurabhsandav.fyersapi.model.response

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = FyersSerializer::class)
public data class FyersResponse<T>(
    val s: String?,
    val code: Int,
    val message: String?,
    val result: T?,
)

internal class FyersSerializer<T>(
    private val dataSerializer: KSerializer<T>,
) : KSerializer<FyersResponse<T>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FyersResponse") {
        element("s", serialDescriptor<String>().nullable)
        element("code", serialDescriptor<Int>().nullable)
        element("message", serialDescriptor<String>().nullable)
        element("result", dataSerializer.descriptor.nullable)
    }

    override fun serialize(
        encoder: Encoder,
        value: FyersResponse<T>,
    ) = error("Not Supported")

    override fun deserialize(decoder: Decoder): FyersResponse<T> {
        // Decoder -> JsonDecoder
        require(decoder is JsonDecoder) // this class can be decoded only by Json
        // JsonDecoder -> JsonElement
        val jsonObject = decoder.decodeJsonElement().jsonObject

        val resultMap = jsonObject.filterKeys { key -> key !in FyersResponseCommonKeys }

        return FyersResponse(
            s = jsonObject["s"]?.jsonPrimitive?.content,
            code = jsonObject["code"]?.jsonPrimitive?.intOrNull!!,
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

public fun FyersResponse<*>.getError(): FyersError = FyersError(FyersError.Type.fromCode(code), message.orEmpty())

public data class FyersError(
    public val type: Type,
    public val message: String,
) {

    public enum class Type {
        TokenExpired,
        TokenInvalid,
        TokenAuthFailed,
        TokenInvalidOrExpired,
        InvalidParameters,
        InvalidOrderId,
        InvalidPositionId,
        OrderPlacementRejected,
        InvalidSymbol,
        InvalidAppId,
        NoPositionToExit,
        RateLimitExceeded,
        MultiLegOrderInvalidInput,
        ;

        public companion object {

            public fun fromCode(code: Int): Type = when (code) {
                -8 -> TokenExpired
                -15 -> TokenInvalid
                -16 -> TokenAuthFailed
                -17 -> TokenInvalidOrExpired
                -50 -> InvalidParameters
                -51 -> InvalidOrderId
                -53 -> InvalidPositionId
                -99 -> OrderPlacementRejected
                -300 -> InvalidSymbol
                -352 -> InvalidAppId
//            -352 -> NoPositionToExit
                -429 -> RateLimitExceeded
                -400 -> MultiLegOrderInvalidInput
                else -> error("Fyers: Unexpected error code $code")
            }
        }
    }
}

public val FyersError.isTokenExpired: Boolean
    get() = type == FyersError.Type.TokenExpired

private val FyersResponseCommonKeys = listOf("s", "code", "message")

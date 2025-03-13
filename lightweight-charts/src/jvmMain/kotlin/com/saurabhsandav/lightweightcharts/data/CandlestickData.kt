package com.saurabhsandav.lightweightcharts.data

import com.saurabhsandav.lightweightcharts.utils.SerializableColor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable(with = CandlestickDataSerializer::class)
sealed interface CandlestickData : WhitespaceData {

    @Serializable
    data class Item(
        override val time: Time,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val color: SerializableColor? = null,
        val borderColor: SerializableColor? = null,
        val wickColor: SerializableColor? = null,
    ) : CandlestickData

    @Serializable
    data class WhiteSpace(
        override val time: Time,
    ) : CandlestickData
}

private object CandlestickDataSerializer : JsonContentPolymorphicSerializer<CandlestickData>(CandlestickData::class) {

    override fun selectDeserializer(element: JsonElement): KSerializer<out CandlestickData> {

        val isItem = (element as? JsonObject)?.containsKey("open") == true

        return when {
            isItem -> CandlestickData.Item.serializer()
            else -> CandlestickData.WhiteSpace.serializer()
        }
    }
}

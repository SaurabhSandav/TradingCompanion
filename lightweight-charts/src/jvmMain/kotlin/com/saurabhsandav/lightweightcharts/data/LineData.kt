package com.saurabhsandav.lightweightcharts.data

import com.saurabhsandav.lightweightcharts.utils.SerializableColor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable(with = LineDataSerializer::class)
sealed interface LineData : WhitespaceData {

    @Serializable
    data class Item(
        override val time: Time,
        override val value: Double,
        val color: SerializableColor? = null,
    ) : SingleValueData,
        LineData

    @Serializable
    data class WhiteSpace(
        override val time: Time,
    ) : WhitespaceData,
        LineData
}

private object LineDataSerializer : JsonContentPolymorphicSerializer<LineData>(LineData::class) {

    override fun selectDeserializer(element: JsonElement): KSerializer<out LineData> {

        val isItem = (element as? JsonObject)?.containsKey("value") == true

        return when {
            isItem -> LineData.Item.serializer()
            else -> LineData.WhiteSpace.serializer()
        }
    }
}

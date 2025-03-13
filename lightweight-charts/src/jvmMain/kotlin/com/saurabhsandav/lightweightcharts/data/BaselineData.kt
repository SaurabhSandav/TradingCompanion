package com.saurabhsandav.lightweightcharts.data

import com.saurabhsandav.lightweightcharts.utils.SerializableColor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable(with = BaselineDataSerializer::class)
sealed interface BaselineData : WhitespaceData {

    @Serializable
    data class Item(
        override val time: Time,
        override val value: Double,
        val topLineColor: SerializableColor? = null,
        val topLineColor1: SerializableColor? = null,
        val topLineColor2: SerializableColor? = null,
        val bottomLineColor: SerializableColor? = null,
        val bottomLineColor1: SerializableColor? = null,
        val bottomLineColor2: SerializableColor? = null,
    ) : SingleValueData,
        BaselineData

    @Serializable
    data class WhiteSpace(
        override val time: Time,
    ) : BaselineData
}

private object BaselineDataSerializer : JsonContentPolymorphicSerializer<BaselineData>(BaselineData::class) {

    override fun selectDeserializer(element: JsonElement): KSerializer<out BaselineData> {

        val isItem = (element as? JsonObject)?.containsKey("value") == true

        return when {
            isItem -> BaselineData.Item.serializer()
            else -> BaselineData.WhiteSpace.serializer()
        }
    }
}

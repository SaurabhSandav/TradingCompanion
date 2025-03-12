package com.saurabhsandav.lightweight_charts.data

import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable(with = HistogramDataSerializer::class)
sealed interface HistogramData : WhitespaceData {

    @Serializable
    data class Item(
        override val time: Time,
        override val value: Double,
        val color: SerializableColor? = null,
    ) : SingleValueData,
        HistogramData

    @Serializable
    data class WhiteSpace(
        override val time: Time,
    ) : HistogramData
}

private object HistogramDataSerializer : JsonContentPolymorphicSerializer<HistogramData>(HistogramData::class) {

    override fun selectDeserializer(element: JsonElement): KSerializer<out HistogramData> {

        val isItem = (element as? JsonObject)?.containsKey("value") == true

        return when {
            isItem -> HistogramData.Item.serializer()
            else -> HistogramData.WhiteSpace.serializer()
        }
    }
}

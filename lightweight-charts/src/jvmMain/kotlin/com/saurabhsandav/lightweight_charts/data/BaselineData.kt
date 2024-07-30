package com.saurabhsandav.lightweight_charts.data

import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.Serializable

@Serializable
sealed interface BaselineData : WhitespaceData {

    @Serializable
    class Item(
        override val time: Time,
        override val value: Double,
        val topLineColor: SerializableColor? = null,
        val topLineColor1: SerializableColor? = null,
        val topLineColor2: SerializableColor? = null,
        val bottomLineColor: SerializableColor? = null,
        val bottomLineColor1: SerializableColor? = null,
        val bottomLineColor2: SerializableColor? = null,
    ) : SingleValueData(), BaselineData

    @Serializable
    class WhiteSpace(
        override val time: Time,
    ) : BaselineData
}

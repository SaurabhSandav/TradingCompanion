package com.saurabhsandav.lightweight_charts.data

import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.Serializable

@Serializable
sealed interface LineData : WhitespaceData {

    @Serializable
    class Item(
        override val time: Time,
        override val value: Double,
        val color: SerializableColor? = null,
    ) : SingleValueData(), LineData

    @Serializable
    class WhiteSpace(
        override val time: Time,
    ) : WhitespaceData, LineData
}

package com.saurabhsandav.lightweightcharts.options.common

import com.saurabhsandav.lightweightcharts.utils.SerializableColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Background {

    @Serializable
    @SerialName("ColorType.Solid")
    data class SolidColor(
        val color: SerializableColor,
    ) : Background()

    @Serializable
    @SerialName("ColorType.VerticalGradient")
    data class VerticalGradientColor(
        val topColor: SerializableColor? = null,
        val bottomColor: SerializableColor? = null,
    ) : Background()
}

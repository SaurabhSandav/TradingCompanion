package com.saurabhsandav.lightweight_charts.data

import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeriesMarker(
    val time: Time,
    val position: SeriesMarkerPosition,
    val shape: SeriesMarkerShape,
    val color: SerializableColor,
    val id: String? = null,
    val text: String? = null,
    val size: Double? = null,
)

@Serializable
enum class SeriesMarkerPosition {

    @SerialName("aboveBar")
    AboveBar,

    @SerialName("belowBar")
    BelowBar,

    @SerialName("inBar")
    InBar,
}

@Serializable
enum class SeriesMarkerShape {

    @SerialName("circle")
    Circle,

    @SerialName("square")
    Square,

    @SerialName("arrowUp")
    ArrowUp,

    @SerialName("arrowDown")
    ArrowDown,
}

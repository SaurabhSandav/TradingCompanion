package com.saurabhsandav.lightweight_charts.data

import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.Serializable

@Serializable
class CandlestickData(
    val time: Time,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val color: SerializableColor? = null,
    val borderColor: SerializableColor? = null,
    val wickColor: SerializableColor? = null,
) : SeriesData

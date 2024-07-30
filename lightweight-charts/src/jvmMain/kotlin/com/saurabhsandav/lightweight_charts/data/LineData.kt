package com.saurabhsandav.lightweight_charts.data

import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.Serializable

@Serializable
class LineData(
    override val time: Time,
    override val value: Double,
    val color: SerializableColor? = null,
) : SingleValueData()

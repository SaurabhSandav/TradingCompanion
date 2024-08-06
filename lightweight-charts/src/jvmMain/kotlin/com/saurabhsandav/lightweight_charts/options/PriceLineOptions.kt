package com.saurabhsandav.lightweight_charts.options

import com.saurabhsandav.lightweight_charts.options.common.LineStyle
import com.saurabhsandav.lightweight_charts.options.common.LineWidth
import com.saurabhsandav.lightweight_charts.utils.SerializableColor
import kotlinx.serialization.Serializable

@Serializable
data class PriceLineOptions(
    val id: String? = null,
    val price: Double? = null,
    val color: SerializableColor? = null,
    val lineWidth: LineWidth? = null,
    val lineStyle: LineStyle? = null,
    val lineVisible: Boolean? = null,
    val axisLabelVisible: Boolean? = null,
    val title: String? = null,
    val axisLabelColor: SerializableColor? = null,
    val axisLabelTextColor: SerializableColor? = null,
)

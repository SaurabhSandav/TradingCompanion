package com.saurabhsandav.lightweightcharts.options

import com.saurabhsandav.lightweightcharts.options.common.LineStyle
import com.saurabhsandav.lightweightcharts.options.common.LineWidth
import com.saurabhsandav.lightweightcharts.utils.SerializableColor
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

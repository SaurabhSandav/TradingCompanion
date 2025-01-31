package com.saurabhsandav.lightweight_charts.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MouseEventParams(
    val time: Time?,
    val logical: Float?,
    val point: Point?,
    val paneIndex: Int?,
    val seriesData: Map<String, JsonElement>,
)

@Serializable
data class Point(
    val x: Float,
    val y: Float,
)

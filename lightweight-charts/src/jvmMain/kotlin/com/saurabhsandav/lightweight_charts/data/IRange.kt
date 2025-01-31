package com.saurabhsandav.lightweight_charts.data

import kotlinx.serialization.Serializable

@Serializable
data class IRange<T>(
    val from: T,
    val to: T,
)

typealias LogicalRange = IRange<Float>

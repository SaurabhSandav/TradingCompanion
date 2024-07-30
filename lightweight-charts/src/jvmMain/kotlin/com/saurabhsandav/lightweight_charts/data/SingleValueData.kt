package com.saurabhsandav.lightweight_charts.data

import kotlinx.serialization.Serializable

@Serializable
abstract class SingleValueData : SeriesData {

    abstract val time: Time

    abstract val value: Double

    companion object {

        operator fun invoke(
            time: Time,
            value: Double,
        ): SingleValueData = object : SingleValueData() {
            override val time: Time = time
            override val value: Double = value
        }
    }
}

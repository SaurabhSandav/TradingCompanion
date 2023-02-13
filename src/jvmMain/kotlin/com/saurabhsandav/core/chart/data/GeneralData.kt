package com.saurabhsandav.core.chart.data

sealed class GeneralData {

    data class BarPrice(val value: Number) : GeneralData()

    data class BarPrices(
        val open: Number,
        val high: Number,
        val low: Number,
        val close: Number,
    ) : GeneralData()
}

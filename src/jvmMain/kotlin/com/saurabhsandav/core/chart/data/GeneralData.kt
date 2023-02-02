package com.saurabhsandav.core.chart.data

import java.math.BigDecimal

sealed class GeneralData {

    data class BarPrice(val value: BigDecimal) : GeneralData()

    data class BarPrices(
        val open: BigDecimal,
        val high: BigDecimal,
        val low: BigDecimal,
        val close: BigDecimal,
    ) : GeneralData()
}

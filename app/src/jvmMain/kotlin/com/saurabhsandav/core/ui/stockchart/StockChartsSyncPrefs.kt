package com.saurabhsandav.core.ui.stockchart

import kotlinx.serialization.Serializable

@Serializable
internal data class StockChartsSyncPrefs(
    val crosshair: Boolean = true,
    val time: Boolean = true,
    val dateRange: Boolean = true,
) {

    companion object {

        val PrefKey = "StockChartsSyncPrefs"
    }
}

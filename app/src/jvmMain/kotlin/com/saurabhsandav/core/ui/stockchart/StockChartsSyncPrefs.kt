package com.saurabhsandav.core.ui.stockchart

import kotlinx.serialization.Serializable

@Serializable
data class StockChartsSyncPrefs(
    val crosshair: Boolean = true,
    val time: Boolean = true,
    val dateRange: Boolean = true,
) {

    companion object {

        internal val PrefKey = "StockChartsSyncPrefs"
    }
}

package com.saurabhsandav.core.ui.stockchart

import kotlinx.coroutines.flow.Flow

interface Plotter<D> {

    val key: String

    val legendLabel: String

    var isEnabled: Boolean

    fun setData(range: IntRange)

    fun update(index: Int)

    fun legendText(chart: StockChart): Flow<String>

    fun onAttach(chart: StockChart)

    fun onDetach(chart: StockChart)
}

package com.saurabhsandav.core.ui.stockchart

import androidx.compose.ui.text.AnnotatedString

interface Plotter<D> {

    val key: String

    val legendLabel: AnnotatedString

    val legendText: AnnotatedString

    var isEnabled: Boolean

    fun setData(data: List<D>)

    fun update(item: D)

    fun onAttach(chart: StockChart)

    fun onDetach(chart: StockChart)
}

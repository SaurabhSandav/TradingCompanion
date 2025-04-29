package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.runtime.Composable

fun interface StockChartWindowDecorator {

    @Composable
    fun Decoration(content: @Composable () -> Unit)

    companion object {

        internal val Default = StockChartWindowDecorator { content -> content() }
    }
}

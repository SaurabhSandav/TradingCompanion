package com.saurabhsandav.core.ui.charts.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.fyerslogin.FyersLoginState
import com.saurabhsandav.core.ui.stockchart.StockChartsState

@Immutable
internal data class ChartsState(
    val chartsState: StockChartsState,
    val fyersLoginWindowState: FyersLoginWindow,
    val errors: List<UIErrorMessage>,
    val eventSink: (ChartsEvent) -> Unit,
) {

    @Immutable
    sealed class FyersLoginWindow {

        @Immutable
        internal data class Open(val fyersLoginState: FyersLoginState) : FyersLoginWindow()

        @Immutable
        data object Closed : FyersLoginWindow()
    }
}

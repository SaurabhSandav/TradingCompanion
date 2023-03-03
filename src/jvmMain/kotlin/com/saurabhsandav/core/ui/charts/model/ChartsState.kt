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
) {

    @Immutable
    sealed class FyersLoginWindow {

        @Immutable
        internal class Open(val fyersLoginState: FyersLoginState) : FyersLoginWindow()

        @Immutable
        object Closed : FyersLoginWindow()
    }
}

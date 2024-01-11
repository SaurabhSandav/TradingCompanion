package com.saurabhsandav.core.ui.tradesfiltersheet.model

import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.OpenClosed
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.Side

internal sealed class TradesFilterEvent {

    data class SetFilter(val filterConfig: FilterConfig) : TradesFilterEvent()

    data class FilterOpenClosed(val openClosed: OpenClosed) : TradesFilterEvent()

    data class FilterSide(val side: Side) : TradesFilterEvent()

    data object ResetFilter : TradesFilterEvent()

    data object ApplyFilter : TradesFilterEvent()
}

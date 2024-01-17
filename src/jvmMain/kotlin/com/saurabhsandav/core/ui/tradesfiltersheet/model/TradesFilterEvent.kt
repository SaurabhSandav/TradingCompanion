package com.saurabhsandav.core.ui.tradesfiltersheet.model

import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.*

internal sealed class TradesFilterEvent {

    data class SetFilter(val filterConfig: FilterConfig) : TradesFilterEvent()

    data class FilterOpenClosed(val openClosed: OpenClosed) : TradesFilterEvent()

    data class FilterSide(val side: Side) : TradesFilterEvent()

    data class FilterDateInterval(val dateInterval: DateInterval) : TradesFilterEvent()

    data class FilterTimeInterval(val timeInterval: TimeInterval) : TradesFilterEvent()

    data class FilterPnl(val pnl: PNL) : TradesFilterEvent()

    data class FilterByNetPnl(val isEnabled: Boolean) : TradesFilterEvent()

    data class FilterNotes(val notes: Notes) : TradesFilterEvent()

    data object ResetFilter : TradesFilterEvent()

    data object ApplyFilter : TradesFilterEvent()
}

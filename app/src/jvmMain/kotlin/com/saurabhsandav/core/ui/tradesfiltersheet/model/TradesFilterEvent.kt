package com.saurabhsandav.core.ui.tradesfiltersheet.model

import com.saurabhsandav.core.trading.record.model.TradeTagId
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.DateInterval
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.Notes
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.OpenClosed
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.PNL
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.Side
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.TimeInterval

internal sealed class TradesFilterEvent {

    data class SetFilter(
        val filterConfig: FilterConfig,
    ) : TradesFilterEvent()

    data class FilterOpenClosed(
        val openClosed: OpenClosed,
    ) : TradesFilterEvent()

    data class FilterSide(
        val side: Side,
    ) : TradesFilterEvent()

    data class FilterDateInterval(
        val dateInterval: DateInterval,
    ) : TradesFilterEvent()

    data class FilterTimeInterval(
        val timeInterval: TimeInterval,
    ) : TradesFilterEvent()

    data class FilterPnl(
        val pnl: PNL,
    ) : TradesFilterEvent()

    data class FilterByNetPnl(
        val isEnabled: Boolean,
    ) : TradesFilterEvent()

    data class FilterNotes(
        val notes: Notes,
    ) : TradesFilterEvent()

    data class AddTag(
        val id: TradeTagId,
    ) : TradesFilterEvent()

    data class RemoveTag(
        val id: TradeTagId,
    ) : TradesFilterEvent()

    data class SetMatchAllTagsEnabled(
        val isEnabled: Boolean,
    ) : TradesFilterEvent()

    data class AddTicker(
        val ticker: String,
    ) : TradesFilterEvent()

    data class RemoveTicker(
        val ticker: String,
    ) : TradesFilterEvent()

    data object ResetFilter : TradesFilterEvent()

    data object ApplyFilter : TradesFilterEvent()
}

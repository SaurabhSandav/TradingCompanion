package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.runtime.Composable
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.OpenClosed

@Composable
internal fun OpenClosedFilterItem(
    openClosed: OpenClosed,
    onOpenClosedChange: (OpenClosed) -> Unit,
) {

    TradeFilterChipGroup(
        title = "Open/Closed",
        expandInitially = openClosed != OpenClosed.All,
    ) {

        TradeFilterChip(
            label = "All",
            selected = openClosed == OpenClosed.All,
            onClick = { onOpenClosedChange(OpenClosed.All) },
        )

        TradeFilterChip(
            label = "Open",
            selected = openClosed == OpenClosed.Open,
            onClick = { onOpenClosedChange(OpenClosed.Open) },
        )

        TradeFilterChip(
            label = "Closed",
            selected = openClosed == OpenClosed.Closed,
            onClick = { onOpenClosedChange(OpenClosed.Closed) },
        )
    }
}

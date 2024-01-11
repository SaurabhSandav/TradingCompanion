package com.saurabhsandav.core.ui.tradesfiltersheet.ui

import androidx.compose.runtime.Composable
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.Side

@Composable
internal fun SideFilterItem(
    side: Side,
    onSideChange: (Side) -> Unit,
) {

    TradeFilterChipGroup(
        title = "Side",
        expandInitially = side != Side.All,
    ) {

        TradeFilterChip(
            label = "All",
            selected = side == Side.All,
            onClick = { onSideChange(Side.All) },
        )

        TradeFilterChip(
            label = "Long",
            selected = side == Side.Long,
            onClick = { onSideChange(Side.Long) },
        )

        TradeFilterChip(
            label = "Short",
            selected = side == Side.Short,
            onClick = { onSideChange(Side.Short) },
        )
    }
}

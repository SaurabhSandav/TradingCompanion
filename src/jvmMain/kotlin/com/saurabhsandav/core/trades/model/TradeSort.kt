package com.saurabhsandav.core.trades.model

sealed class TradeSort {

    data object EntryDesc : TradeSort()

    data object OpenDescEntryDesc : TradeSort()
}

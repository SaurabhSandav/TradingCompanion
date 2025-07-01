package com.saurabhsandav.trading.record.model

sealed class TradeSort {

    data object EntryDesc : TradeSort()

    data object OpenDescEntryDesc : TradeSort()
}

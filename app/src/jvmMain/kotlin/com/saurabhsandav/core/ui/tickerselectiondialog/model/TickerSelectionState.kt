package com.saurabhsandav.core.ui.tickerselectiondialog.model

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

internal data class TickerSelectionState(
    val tickers: Flow<PagingData<String>>,
    val eventSink: (TickerSelectionEvent) -> Unit,
)

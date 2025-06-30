package com.saurabhsandav.core.ui.symbolselectiondialog.model

import androidx.paging.PagingData
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.coroutines.flow.Flow

internal data class SymbolSelectionState(
    val symbols: Flow<PagingData<SymbolId>>,
    val eventSink: (SymbolSelectionEvent) -> Unit,
)

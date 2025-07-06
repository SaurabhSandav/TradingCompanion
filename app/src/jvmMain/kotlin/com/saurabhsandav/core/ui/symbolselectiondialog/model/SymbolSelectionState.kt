package com.saurabhsandav.core.ui.symbolselectiondialog.model

import androidx.compose.foundation.text.input.TextFieldState
import androidx.paging.PagingData
import com.saurabhsandav.trading.core.SymbolId
import kotlinx.coroutines.flow.Flow

internal data class SymbolSelectionState(
    val symbols: Flow<PagingData<Symbol>>,
    val selectedSymbol: Symbol?,
    val filterQuery: TextFieldState,
    val eventSink: (SymbolSelectionEvent) -> Unit,
) {

    internal data class Symbol(
        val id: SymbolId,
        val exchange: String,
        val type: String,
        val title: String,
        val description: String?,
    )
}

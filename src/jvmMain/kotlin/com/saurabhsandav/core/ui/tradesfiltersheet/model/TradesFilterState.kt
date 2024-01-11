package com.saurabhsandav.core.ui.tradesfiltersheet.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class TradesFilterState(
    val filterConfig: FilterConfig,
    val eventSink: (TradesFilterEvent) -> Unit,
)

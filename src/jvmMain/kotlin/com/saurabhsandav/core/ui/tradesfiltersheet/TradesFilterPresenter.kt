package com.saurabhsandav.core.ui.tradesfiltersheet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig
import com.saurabhsandav.core.ui.tradesfiltersheet.model.FilterConfig.*
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterEvent.*
import com.saurabhsandav.core.ui.tradesfiltersheet.model.TradesFilterState
import kotlinx.coroutines.CoroutineScope

internal class TradesFilterPresenter(
    coroutineScope: CoroutineScope,
    initialFilterConfig: FilterConfig,
    private val onFilterChange: (FilterConfig) -> Unit,
) {

    private var filterConfig by mutableStateOf(initialFilterConfig)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule TradesFilterState(
            filterConfig = filterConfig,
            eventSink = ::onEvent,
        )
    }

    val errors = mutableStateListOf<UIErrorMessage>()

    private fun onEvent(event: TradesFilterEvent) {

        when (event) {
            is SetFilter -> onSetFilter(event.filterConfig)
            is FilterOpenClosed -> onFilterOpenClosed(event.openClosed)
            is FilterSide -> onFilterSide(event.side)
            is FilterPnl -> onFilterPnl(event.pnl)
            is FilterByNetPnl -> onFilterByNetPnl(event.isEnabled)
            ResetFilter -> onResetFilter()
            ApplyFilter -> onApplyFilter()
        }
    }

    private fun onSetFilter(filterConfig: FilterConfig) {
        this.filterConfig = filterConfig
    }

    private fun onFilterOpenClosed(openClosed: OpenClosed) {
        filterConfig = filterConfig.copy(openClosed = openClosed)
    }

    private fun onFilterSide(side: Side) {
        filterConfig = filterConfig.copy(side = side)
    }

    private fun onFilterPnl(pnl: PNL) {
        filterConfig = filterConfig.copy(pnl = pnl)
    }

    private fun onFilterByNetPnl(isEnabled: Boolean) {
        filterConfig = filterConfig.copy(filterByNetPnl = isEnabled)
    }

    private fun onResetFilter() {
        filterConfig = FilterConfig()
    }

    private fun onApplyFilter() {
        onFilterChange(filterConfig)
    }
}

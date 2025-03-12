package com.saurabhsandav.core.ui.tradeexecutions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.DeleteExecutions
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.EditExecution
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.LockExecutions
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.NewExecution
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.NewExecutionFromExisting

internal class TradeExecutionsLandingSwitcherItem(
    tradeExecutionsModule: TradeExecutionsModule,
) : LandingSwitcherItem {

    private val presenter = tradeExecutionsModule.presenter()

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TradeExecutionsScreen(
            onNewExecution = { state.eventSink(NewExecution) },
            executionEntries = state.executionEntries,
            selectionManager = state.selectionManager,
            onNewExecutionFromExisting = { state.eventSink(NewExecutionFromExisting(it)) },
            onLockExecutions = { ids -> state.eventSink(LockExecutions(ids)) },
            onEditExecution = { state.eventSink(EditExecution(it)) },
            onDeleteExecutions = { ids -> state.eventSink(DeleteExecutions(ids)) },
        )
    }
}

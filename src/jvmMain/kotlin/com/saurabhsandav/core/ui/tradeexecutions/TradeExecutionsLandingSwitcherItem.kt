package com.saurabhsandav.core.ui.tradeexecutions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.landing.LandingSwitcherItem
import com.saurabhsandav.core.ui.landing.model.LandingState.TradeExecutionFormWindowParams
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsEvent.*
import kotlinx.coroutines.CoroutineScope

internal class TradeExecutionsLandingSwitcherItem(
    coroutineScope: CoroutineScope,
    appModule: AppModule,
    executionFormWindowsManager: AppWindowsManager<TradeExecutionFormWindowParams>,
) : LandingSwitcherItem {

    private val presenter = TradeExecutionsPresenter(coroutineScope, appModule, executionFormWindowsManager)

    @Composable
    override fun Content() {

        val state by presenter.state.collectAsState()

        TradeExecutionsScreen(
            onNewExecution = { state.eventSink(NewExecution) },
            items = state.items,
            selectionManager = state.selectionManager,
            onNewExecutionFromExisting = { state.eventSink(NewExecutionFromExisting(it)) },
            onLockExecutions = { ids -> state.eventSink(LockExecutions(ids)) },
            onEditExecution = { state.eventSink(EditExecution(it)) },
            onDeleteExecutions = { ids -> state.eventSink(DeleteExecutions(ids)) },
            errors = state.errors,
        )
    }
}

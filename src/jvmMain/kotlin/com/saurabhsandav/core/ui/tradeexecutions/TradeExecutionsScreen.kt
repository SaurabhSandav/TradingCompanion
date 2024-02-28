package com.saurabhsandav.core.ui.tradeexecutions

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.ExecutionsList
import com.saurabhsandav.core.ui.tradeexecutions.ui.TradeExecutionsSelectionBar
import com.saurabhsandav.core.ui.tradeexecutions.ui.TradeExecutionsTable

@Composable
internal fun TradeExecutionsScreen(
    onNewExecution: () -> Unit,
    executionsList: ExecutionsList,
    selectionManager: SelectionManager<TradeExecutionId>,
    canSelectionLock: Boolean,
    onNewExecutionFromExisting: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecutions: (List<TradeExecutionId>) -> Unit,
    onDeleteExecutions: (List<TradeExecutionId>) -> Unit,
    errors: List<UIErrorMessage>,
) {

    // Set window title
    WindowTitle("Trade Executions")

    val snackbarHostState = remember { SnackbarHostState() }

    Column {

        Scaffold(
            modifier = Modifier.weight(1F),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {

                ExtendedFloatingActionButton(onClick = onNewExecution) {
                    Text(text = "New Trade Execution")
                }
            },
        ) {

            TradeExecutionsTable(
                executionsList = executionsList,
                isMarked = { id -> id in selectionManager.selection },
                onClickExecution = { id ->

                    // Ignore if not in selection mode
                    if (selectionManager.selection.isNotEmpty()) {
                        selectionManager.select(id)
                    }
                },
                onMarkExecution = selectionManager::select,
                onNewExecution = onNewExecutionFromExisting,
                onEditExecution = onEditExecution,
                onLockExecution = { id -> onLockExecutions(listOf(id)) },
                onDeleteExecution = { id -> onDeleteExecutions(listOf(id)) },
            )

            // Errors
            errors.forEach { errorMessage ->

                ErrorSnackbar(snackbarHostState, errorMessage)
            }
        }

        TradeExecutionsSelectionBar(
            selectionManager = selectionManager,
            canSelectionLock = canSelectionLock,
            onLockExecutions = onLockExecutions,
            onDeleteExecutions = onDeleteExecutions,
        )
    }
}

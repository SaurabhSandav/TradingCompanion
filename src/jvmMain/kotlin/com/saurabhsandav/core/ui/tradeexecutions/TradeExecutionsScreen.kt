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
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry
import com.saurabhsandav.core.ui.tradeexecutions.ui.TradeExecutionsSelectionBar
import com.saurabhsandav.core.ui.tradeexecutions.ui.TradeExecutionsTable
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradeExecutionsScreen(
    onNewExecution: () -> Unit,
    todayExecutions: ImmutableList<TradeExecutionEntry>,
    pastExecutions: ImmutableList<TradeExecutionEntry>,
    selectionManager: SelectionManager<TradeExecutionEntry>,
    onNewExecutionFromExisting: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecutions: (List<TradeExecutionId>) -> Unit,
    onDeleteExecutions: (List<TradeExecutionId>) -> Unit,
    errors: ImmutableList<UIErrorMessage>,
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
                todayExecutions = todayExecutions,
                pastExecutions = pastExecutions,
                isMarked = { entry -> entry in selectionManager.selection },
                onClickExecution = { entry ->

                    // Ignore if not in selection mode
                    if (selectionManager.selection.isNotEmpty()) {
                        selectionManager.select(entry)
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
            onLockExecutions = onLockExecutions,
            onDeleteExecutions = onDeleteExecutions,
        )
    }
}

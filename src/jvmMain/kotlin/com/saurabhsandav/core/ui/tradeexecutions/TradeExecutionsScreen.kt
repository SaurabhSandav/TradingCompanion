package com.saurabhsandav.core.ui.tradeexecutions

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.ErrorSnackbar
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.UIErrorMessage
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.*
import com.saurabhsandav.core.ui.tradeexecutions.ui.TradeExecutionsSelectionBar
import com.saurabhsandav.core.ui.tradeexecutions.ui.TradeExecutionsTable
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TradeExecutionsScreen(
    onNewExecution: () -> Unit,
    items: ImmutableList<TradeExecutionListItem>,
    selectionManager: SelectionManager<TradeExecutionEntry>,
    onNewExecutionFromExisting: (ProfileTradeExecutionId) -> Unit,
    onEditExecution: (ProfileTradeExecutionId) -> Unit,
    onLockExecutions: (List<ProfileTradeExecutionId>) -> Unit,
    onDeleteExecutions: (List<ProfileTradeExecutionId>) -> Unit,
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
                items = items,
                isMarked = { entry -> entry in selectionManager.selection },
                onClickExecution = { entry -> selectionManager.select(entry) },
                onMarkExecution = { entry -> selectionManager.multiSelect(entry) },
                onNewExecution = onNewExecutionFromExisting,
                onEditExecution = onEditExecution,
                onLockExecution = { profileExecutionId -> onLockExecutions(listOf(profileExecutionId)) },
                onDeleteExecution = { profileExecutionId -> onDeleteExecutions(listOf(profileExecutionId)) },
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

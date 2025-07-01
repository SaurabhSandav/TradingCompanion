package com.saurabhsandav.core.ui.tradeexecutions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.PagingData
import com.saurabhsandav.core.ui.common.PrimaryOptionsBar
import com.saurabhsandav.core.ui.common.SelectionManager
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry
import com.saurabhsandav.core.ui.tradeexecutions.ui.TradeExecutionsSelectionBar
import com.saurabhsandav.core.ui.tradeexecutions.ui.TradeExecutionsTable
import com.saurabhsandav.trading.record.model.TradeExecutionId
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TradeExecutionsScreen(
    onNewExecution: () -> Unit,
    executionEntries: Flow<PagingData<TradeExecutionEntry>>,
    selectionManager: SelectionManager<TradeExecutionId>,
    onNewExecutionFromExisting: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecutions: (List<TradeExecutionId>) -> Unit,
    onDeleteExecutions: (List<TradeExecutionId>) -> Unit,
) {

    // Set window title
    WindowTitle("Trade Executions")

    Column {

        Scaffold(
            modifier = Modifier.weight(1F),
        ) { paddingValues ->

            Column(Modifier.padding(paddingValues)) {

                PrimaryOptionsBar {

                    Button(
                        onClick = onNewExecution,
                        shape = MaterialTheme.shapes.small,
                        content = { Text("New Execution") },
                    )
                }

                HorizontalDivider()

                TradeExecutionsTable(
                    executionEntries = executionEntries,
                    isMarked = { id -> id in selectionManager.selection },
                    onClickExecution = { id ->

                        // Ignore if not in selection mode
                        if (selectionManager.selection.isNotEmpty()) {
                            selectionManager.select(id)
                        }
                    },
                    onMarkExecution = selectionManager::select,
                    onNewExecutionFromExisting = onNewExecutionFromExisting,
                    onEditExecution = onEditExecution,
                    onLockExecution = { id -> onLockExecutions(listOf(id)) },
                    onDeleteExecution = { id -> onDeleteExecutions(listOf(id)) },
                )
            }
        }

        TradeExecutionsSelectionBar(
            selectionManager = selectionManager,
            onLockExecutions = onLockExecutions,
            onDeleteExecutions = onDeleteExecutions,
        )
    }
}

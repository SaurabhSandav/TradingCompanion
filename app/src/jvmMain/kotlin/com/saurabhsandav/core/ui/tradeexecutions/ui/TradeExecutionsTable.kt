package com.saurabhsandav.core.ui.tradeexecutions.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import com.saurabhsandav.core.thirdparty.paging.compose.LazyPagingItems
import com.saurabhsandav.core.thirdparty.paging.compose.collectAsLazyPagingItems
import com.saurabhsandav.core.thirdparty.paging.compose.itemContentType
import com.saurabhsandav.core.thirdparty.paging.compose.itemKey
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.ListLoadStateIndicator
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Weight
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.content
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry.Item
import com.saurabhsandav.core.ui.tradeexecutions.model.TradeExecutionsState.TradeExecutionEntry.Section
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TradeExecutionsTable(
    executionEntries: Flow<PagingData<TradeExecutionEntry>>,
    isMarked: (TradeExecutionId) -> Boolean,
    onClickExecution: (TradeExecutionId) -> Unit,
    onMarkExecution: (TradeExecutionId) -> Unit,
    onNewExecutionFromExisting: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecution: (TradeExecutionId) -> Unit,
    onDeleteExecution: (TradeExecutionId) -> Unit,
) {

    val items = executionEntries.collectAsLazyPagingItems()

    ListLoadStateIndicator(
        state = {
            when {
                items.loadState.refresh is LoadState.Loading -> loading()
                items.itemCount == 0 -> empty()
                else -> loaded()
            }
        },
        emptyText = { "No Executions" },
    ) {

        TradeExecutionsTable(
            items = items,
            isMarked = isMarked,
            onClickExecution = onClickExecution,
            onMarkExecution = onMarkExecution,
            onNewExecutionFromExisting = onNewExecutionFromExisting,
            onEditExecution = onEditExecution,
            onLockExecution = onLockExecution,
            onDeleteExecution = onDeleteExecution,
        )
    }
}

@Composable
private fun TradeExecutionsTable(
    items: LazyPagingItems<TradeExecutionEntry>,
    isMarked: (TradeExecutionId) -> Boolean,
    onClickExecution: (TradeExecutionId) -> Unit,
    onMarkExecution: (TradeExecutionId) -> Unit,
    onNewExecutionFromExisting: (TradeExecutionId) -> Unit,
    onEditExecution: (TradeExecutionId) -> Unit,
    onLockExecution: (TradeExecutionId) -> Unit,
    onDeleteExecution: (TradeExecutionId) -> Unit,
) {

    LazyTable(
        headerContent = { Header() },
    ) {

        items(
            count = items.itemCount,
            key = items.itemKey { entry ->

                when (entry) {
                    is Section -> "Section_${entry.isToday}"
                    is Item -> entry.id
                }
            },
            contentType = items.itemContentType { entry -> entry.javaClass },
        ) { index ->

            when (val entry = items[index]!!) {
                is Section -> Section(
                    modifier = Modifier.animateItem(),
                    section = entry,
                )

                is Item -> Item(
                    modifier = Modifier.animateItem(),
                    item = entry,
                    isMarked = isMarked(entry.id),
                    onClick = { onClickExecution(entry.id) },
                    onLongClick = { onMarkExecution(entry.id) },
                    onMarkExecution = { onMarkExecution(entry.id) },
                    onNewExecution = { onNewExecutionFromExisting(entry.id) },
                    onEditExecution = { onEditExecution(entry.id) },
                    onLockExecution = { onLockExecution(entry.id) },
                    onDeleteExecution = { onDeleteExecution(entry.id) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.Header() {

    TradeExecutionTableSchema.SimpleHeader {
        broker.text { "Broker" }
        ticker.text { "Ticker" }
        quantity.text { "Quantity" }
        side.text { "Side" }
        price.text { "Price" }
        time.text { "Time" }
    }
}

@Composable
private fun Section(
    modifier: Modifier,
    section: Section,
) {

    ListItem(
        modifier = Modifier.padding(MaterialTheme.dimens.listItemPadding).then(modifier),
        headlineContent = {
            Text(
                text = if (section.isToday) "Today" else "Past",
                style = MaterialTheme.typography.headlineLarge,
            )
        },
        supportingContent = {

            val count by section.count.collectAsState("")

            Text(
                text = "$count Executions",
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )

    HorizontalDivider()
}

@Composable
private fun Item(
    modifier: Modifier,
    item: Item,
    isMarked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMarkExecution: () -> Unit,
    onNewExecution: () -> Unit,
    onEditExecution: () -> Unit,
    onLockExecution: () -> Unit,
    onDeleteExecution: () -> Unit,
) {

    var showLockConfirmationDialog by state { false }
    var showDeleteConfirmationDialog by state { false }

    // Passing the animateItem() modifier to ListItem doesn't work.
    // Use Box to workaround as the ContextMenuArea doesn't have a modifier parameter.
    Column(modifier) {

        ContextMenuArea(
            items = {

                buildList {
                    add(ContextMenuItem("New", onNewExecution))

                    if (!item.locked) {
                        addAll(
                            listOf(
                                ContextMenuItem("Lock") { showLockConfirmationDialog = true },
                                ContextMenuItem("Edit", onEditExecution),
                                ContextMenuItem("Delete") { showDeleteConfirmationDialog = true },
                            ),
                        )
                    }
                }
            },
        ) {

            TradeExecutionTableSchema.SimpleRow(
                onClick = onClick,
                onLongClick = onLongClick,
            ) {
                select.content {

                    AnimatedVisibility(!item.locked) {

                        Checkbox(
                            checked = isMarked,
                            onCheckedChange = { onMarkExecution() },
                        )
                    }
                }
                broker.text { item.broker }
                ticker.text { item.ticker }
                quantity.text { item.quantity }
                side.content {

                    Text(
                        text = item.side,
                        color = if (item.side == "BUY") AppColor.ProfitGreen else AppColor.LossRed,
                    )
                }
                price.text { item.price }
                time.text { item.timestamp }
            }
        }

        HorizontalDivider()
    }

    if (showLockConfirmationDialog) {

        ConfirmationDialog(
            text = "Are you sure you want to lock the execution?",
            onDismiss = { showLockConfirmationDialog = false },
            onConfirm = {
                showLockConfirmationDialog = false
                onLockExecution()
            },
        )
    }

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            subject = "execution",
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDeleteExecution,
        )
    }
}

private object TradeExecutionTableSchema : TableSchema() {

    val select = cell(Fixed(48.dp))
    val broker = cell(Weight(2F))
    val ticker = cell(Weight(1.7F))
    val quantity = cell()
    val side = cell()
    val price = cell()
    val time = cell(Weight(2.2F))
}

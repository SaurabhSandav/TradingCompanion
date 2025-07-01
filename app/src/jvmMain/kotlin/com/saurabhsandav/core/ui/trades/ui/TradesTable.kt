package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import com.saurabhsandav.core.trading.record.model.TradeId
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.ListLoadStateIndicator
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Weight
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.content
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trades.model.TradesState.Stats
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry.Item
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry.Item.Duration.Closed
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry.Item.Duration.Open
import com.saurabhsandav.core.ui.trades.model.TradesState.TradeEntry.Section
import com.saurabhsandav.paging.compose.LazyPagingItems
import com.saurabhsandav.paging.compose.collectAsLazyPagingItems
import com.saurabhsandav.paging.compose.itemContentType
import com.saurabhsandav.paging.compose.itemKey
import kotlinx.coroutines.flow.Flow

@Composable
internal fun TradesTable(
    tradeEntries: Flow<PagingData<TradeEntry>>,
    isMarked: (TradeId) -> Boolean,
    onMarkExecution: (TradeId) -> Unit,
    onOpenDetails: (TradeId) -> Unit,
    onOpenChart: (TradeId) -> Unit,
) {

    val items = tradeEntries.collectAsLazyPagingItems()

    ListLoadStateIndicator(
        state = {
            when {
                items.loadState.refresh is LoadState.Loading -> loading()
                items.itemCount == 0 -> empty()
                else -> loaded()
            }
        },
        emptyText = { "No Trades" },
    ) {

        TradesTable(
            items = items,
            isMarked = isMarked,
            onMarkExecution = onMarkExecution,
            onOpenDetails = onOpenDetails,
            onOpenChart = onOpenChart,
        )
    }
}

@Composable
private fun TradesTable(
    items: LazyPagingItems<TradeEntry>,
    isMarked: (TradeId) -> Boolean,
    onMarkExecution: (TradeId) -> Unit,
    onOpenDetails: (TradeId) -> Unit,
    onOpenChart: (TradeId) -> Unit,
) {

    LazyTable(
        headerContent = { Header() },
    ) {

        items(
            count = items.itemCount,
            key = items.itemKey { entry ->

                when (entry) {
                    is Section -> "Section_${entry.type}"
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

                is Item -> TradeItem(
                    modifier = Modifier.animateItem(),
                    item = entry,
                    isMarked = isMarked(entry.id),
                    onMarkExecution = { onMarkExecution(entry.id) },
                    onOpenDetails = { onOpenDetails(entry.id) },
                    onOpenChart = { onOpenChart(entry.id) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.Header() {

    TradeTableSchema.SimpleHeader {
        id.text { "ID" }
        broker.text { "Broker" }
        ticker.text { "Ticker" }
        side.text { "Side" }
        quantity.text { "Quantity" }
        avgEntry.text { "Avg. Entry" }
        avgExit.text { "Avg. Exit" }
        entryTime.text { "Entry Time" }
        duration.text { "Duration" }
        pnl.text { "PNL" }
        netPnl.text { "Net PNL" }
        fees.text { "Fees" }
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
                text = when (section.type) {
                    Section.Type.Open -> "Open"
                    Section.Type.Today -> "Today"
                    Section.Type.Past -> "Past"
                    Section.Type.All -> "All"
                    Section.Type.Filtered -> "Filtered"
                },
                style = MaterialTheme.typography.headlineLarge,
            )
        },
        supportingContent = {

            val count by section.count.collectAsState("")

            Text(
                text = "$count Trades",
                style = MaterialTheme.typography.labelLarge,
            )
        },
        trailingContent = section.stats?.let {
            {
                val stats = it.collectAsState(null).value

                if (stats != null) StatsStrip(stats)
            }
        },
    )

    HorizontalDivider()
}

@Composable
private fun TradeItem(
    modifier: Modifier,
    item: Item,
    isMarked: Boolean,
    onMarkExecution: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenChart: () -> Unit,
) {

    // Passing the animateItem() modifier to ListItem doesn't work.
    // Use Box to workaround as the ContextMenuArea doesn't have a modifier parameter.
    Column(modifier) {

        ContextMenuArea(
            items = {
                listOf(
                    ContextMenuItem("Details", onOpenDetails),
                    ContextMenuItem("Chart", onOpenChart),
                )
            },
        ) {

            TradeTableSchema.SimpleRow(
                onClick = onOpenDetails,
            ) {
                select.content {

                    Checkbox(
                        checked = isMarked,
                        onCheckedChange = { onMarkExecution() },
                    )
                }
                id.text { item.id.toString() }
                broker.text { item.broker }
                ticker.text { item.ticker }
                side.content {
                    Text(item.side, color = if (item.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
                }
                quantity.text { item.quantity }
                avgEntry.text { item.entry }
                avgExit.text { item.exit ?: "NA" }
                entryTime.text { item.entryTime }
                duration.content {

                    Text(
                        text = when (val duration = item.duration) {
                            is Open -> duration.flow.collectAsState("").value
                            is Closed -> duration.str
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                pnl.content {
                    Text(item.pnl, color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                netPnl.content {
                    Text(item.netPnl, color = if (item.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                fees.text { item.fees }
            }
        }

        HorizontalDivider()
    }
}

@Composable
private fun StatsStrip(stats: Stats) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(
                text = stats.pnl,
                style = MaterialTheme.typography.titleMedium,
                color = if (stats.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
            )

            Text(
                text = "PNL",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(
                text = stats.netPnl,
                style = MaterialTheme.typography.titleMedium,
                color = if (stats.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed,
            )

            Text(
                text = "Net PNL",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private object TradeTableSchema : TableSchema() {

    val select = cell(Fixed(48.dp))
    val id = cell(Fixed(48.dp))
    val broker = cell(Weight(2F))
    val ticker = cell(Weight(1.7F))
    val side = cell()
    val quantity = cell()
    val avgEntry = cell()
    val avgExit = cell()
    val entryTime = cell(Weight(2.2F))
    val duration = cell(Weight(1.5F))
    val pnl = cell()
    val netPnl = cell()
    val fees = cell()
}

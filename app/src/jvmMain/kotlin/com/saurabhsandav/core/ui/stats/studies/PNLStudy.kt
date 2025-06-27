package com.saurabhsandav.core.ui.stats.studies

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Weight
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.content
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.paging.compose.collectAsLazyPagingItems
import com.saurabhsandav.paging.compose.itemKey
import com.saurabhsandav.trading.record.model.TradeFilter
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeSort
import com.saurabhsandav.trading.record.rValueAt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

internal class PNLStudy(
    profileId: ProfileId,
    tradingProfiles: TradingProfiles,
) : Study {

    @Composable
    override fun render() {

        val items = data.collectAsLazyPagingItems()

        LazyTable(
            modifier = Modifier.fillMaxSize(),
            headerContent = {

                Schema.SimpleHeader {
                    id.text { "ID" }
                    ticker.text { "Ticker" }
                    side.text { "Side" }
                    quantity.text { "Quantity" }
                    avgEntry.text { "Avg. Entry" }
                    avgExit.text { "Avg. Exit" }
                    entryTime.text { "Entry Time" }
                    duration.text { "Duration" }
                    stop.text { "Stop" }
                    target.text { "Target" }
                    pnl.text { "PNL" }
                    netPnl.text { "Net PNL" }
                    fees.text { "Fees" }
                    rValue.text { "R" }
                }
            },
        ) {

            items(
                count = items.itemCount,
                key = items.itemKey { it.id },
            ) { index ->

                val item = items[index]!!
                val generated = item.generated.collectAsState(null).value

                Column(Modifier.animateItem()) {

                    Schema.SimpleRow {
                        id.text { item.id.value.toString() }
                        ticker.text { item.ticker }
                        side.content {
                            Text(item.side, color = if (item.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
                        }
                        quantity.text { item.quantity }
                        avgEntry.text { item.entry }
                        avgExit.text { item.exit }
                        entryTime.text { item.entryTime }
                        duration.text { item.duration }

                        if (generated != null) {
                            stop.text { generated.stop }
                            target.text { generated.target }
                        }

                        pnl.content {
                            Text(
                                item.pnl,
                                color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                            )
                        }
                        netPnl.content {
                            Text(
                                item.netPnl,
                                color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                            )
                        }
                        fees.text { item.fees }

                        if (generated != null) {
                            rValue.text { generated.rValue }
                        }
                    }

                    HorizontalDivider()
                }
            }
        }
    }

    private val data: Flow<PagingData<Item>> = flow {

        val pagingConfig = PagingConfig(
            pageSize = 70,
            enablePlaceholders = false,
            maxSize = 300,
        )

        val tradingRecord = tradingProfiles.getRecord(profileId)

        Pager(
            config = pagingConfig,
            pagingSourceFactory = {

                tradingRecord.trades.getDisplayFilteredPagingSource(
                    filter = TradeFilter(isClosed = true),
                    sort = TradeSort.EntryDesc,
                )
            },
        ).flow.map { pagingData ->

            pagingData.map { trade ->

                val durationStr = run {

                    val durationSeconds = (trade.exitTimestamp!! - trade.entryTimestamp).inWholeSeconds

                    "%02d:%02d:%02d".format(
                        durationSeconds / 3600,
                        (durationSeconds % 3600) / 60,
                        durationSeconds % 60,
                    )
                }

                Item(
                    id = trade.id,
                    ticker = trade.ticker,
                    side = trade.side.strValue.uppercase(),
                    quantity = trade.quantity.toPlainString(),
                    entry = trade.averageEntry.toPlainString(),
                    exit = trade.averageExit!!.toPlainString(),
                    entryTime = trade
                        .entryTimestamp
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(TradeDateTimeFormat),
                    duration = durationStr,
                    pnl = trade.pnl.toPlainString(),
                    isProfitable = trade.pnl > BigDecimal.ZERO,
                    netPnl = trade.netPnl.toPlainString(),
                    isNetProfitable = trade.netPnl > BigDecimal.ZERO,
                    fees = (trade.pnl - trade.netPnl).toPlainString(),
                    generated = combine(
                        tradingRecord.stops.getPrimary(trade.id),
                        tradingRecord.targets.getPrimary(trade.id),
                    ) { stop, target ->

                        val rValue = stop?.let { trade.rValueAt(pnl = trade.pnl, stop = it) }

                        Generated(
                            stop = stop?.price?.toPlainString() ?: "NA",
                            target = target?.price?.toPlainString() ?: "NA",
                            rValue = rValue?.let { "${it}R" }.orEmpty(),
                        )
                    },
                )
            }
        }.emitInto(this)
    }

    private data class Item(
        val id: TradeId,
        val ticker: String,
        val side: String,
        val quantity: String,
        val entry: String,
        val exit: String,
        val entryTime: String,
        val duration: String,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
        val fees: String,
        val generated: Flow<Generated>,
    )

    private data class Generated(
        val stop: String,
        val target: String,
        val rValue: String,
    )

    private object Schema : TableSchema() {

        val id = cell(Fixed(48.dp))
        val ticker = cell(Weight(1.7F))
        val side = cell()
        val quantity = cell()
        val avgEntry = cell()
        val avgExit = cell()
        val entryTime = cell(Weight(2.2F))
        val duration = cell(Weight(1.5F))
        val stop = cell()
        val target = cell()
        val pnl = cell()
        val netPnl = cell()
        val fees = cell()
        val rValue = cell()
    }

    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<PNLStudy> {

        override val name: String = "PNL"

        override fun create() = PNLStudy(profileId, tradingProfiles)
    }
}

package com.saurabhsandav.core.ui.stats.studies

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.SimpleTooltipBox
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.common.getSymbolTitle
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Fixed
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.content
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.stats.StatsGraph
import com.saurabhsandav.core.utils.emitInto
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.paging.compose.collectAsLazyPagingItems
import com.saurabhsandav.paging.compose.itemKey
import com.saurabhsandav.trading.record.TradeDisplay
import com.saurabhsandav.trading.record.TradeExcursions
import com.saurabhsandav.trading.record.TradeStop
import com.saurabhsandav.trading.record.model.TradeFilter
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeSort
import com.saurabhsandav.trading.record.rValueAt
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime

internal class PNLExcursionStudy(
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
                    symbol.text { "Symbol" }
                    side.text { "Side" }
                    quantity.text { "Quantity" }
                    avgEntry.text { "Avg. Entry" }
                    avgExit.text { "Avg. Exit" }
                    entryTime.text { "Entry Time" }
                    duration.text { "Duration" }
                    stop.text { "Stop" }
                    target.text { "Target" }
                    pnl.text { "PNL" }
                    excursionsInTrade.content {

                        SimpleTooltipBox("Excursions In Trade") {
                            Text("In Trade")
                        }
                    }
                    excursionsInSession.content {

                        SimpleTooltipBox("Excursions In Session") {
                            Text("In Session")
                        }
                    }
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
                        symbol.text { item.ticker }
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
                            pnl.content {
                                Text(
                                    generated.pnl,
                                    color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                                )
                            }
                            excursionsInTrade.content { Text(generated.inTrade) }
                            excursionsInSession.content { Text(generated.inSession) }
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
                    ticker = trade.getSymbolTitle(),
                    side = trade.side.strValue.uppercase(),
                    quantity = trade.quantity.toString(),
                    entry = trade.averageEntry.toString(),
                    exit = trade.averageExit!!.toString(),
                    entryTime = trade
                        .entryTimestamp
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .format(TradeDateTimeFormat),
                    duration = durationStr,
                    isProfitable = trade.pnl > KBigDecimal.Zero,
                    generated = combine(
                        tradingRecord.stops.getPrimary(trade.id),
                        tradingRecord.targets.getPrimary(trade.id),
                        tradingRecord.excursions.get(trade.id),
                    ) { stop, target, excursions ->

                        val rValue = stop?.let { trade.rValueAt(pnl = trade.pnl, stop = it) }
                        val rValueStr = rValue?.let { " | ${it}R" }.orEmpty()

                        Generated(
                            stop = stop?.price?.toString() ?: "NA",
                            target = target?.price?.toString() ?: "NA",
                            pnl = "${trade.pnl}$rValueStr",
                            inTrade = trade.buildExcursionString(stop, excursions, true),
                            inSession = trade.buildExcursionString(stop, excursions, false),
                        )
                    },
                )
            }
        }.emitInto(this)
    }

    private fun TradeDisplay.buildExcursionString(
        stop: TradeStop?,
        excursions: TradeExcursions?,
        inTrade: Boolean,
    ): AnnotatedString {

        excursions ?: return AnnotatedString("NA")

        val maePrice = when {
            inTrade -> excursions.tradeMaePrice
            else -> excursions.sessionMaePrice
        }

        val mfePrice = when {
            inTrade -> excursions.tradeMfePrice
            else -> excursions.sessionMfePrice
        }

        val maePnl = when {
            inTrade -> excursions.tradeMaePnl
            else -> excursions.sessionMaePnl
        }

        val mfePnl = when {
            inTrade -> excursions.tradeMfePnl
            else -> excursions.sessionMfePnl
        }

        val maeRStr = when {
            inTrade -> getRString(excursions.tradeMaePnl, stop)
            else -> getRString(excursions.sessionMaePnl, stop)
        }

        val mfeRStr = when {
            inTrade -> getRString(excursions.tradeMfePnl, stop)
            else -> getRString(excursions.sessionMfePnl, stop)
        }

        return buildAnnotatedString {
            withStyle(SpanStyle(color = AppColor.LossRed)) {
                appendLine("MAE: $maePrice | $maePnl$maeRStr")
            }
            withStyle(SpanStyle(color = AppColor.ProfitGreen)) {
                appendLine("MFE: $mfePrice | $mfePnl$mfeRStr")
            }
        }
    }

    private fun TradeDisplay.getRString(
        pnl: KBigDecimal,
        stop: TradeStop?,
    ): String {

        stop ?: return ""

        val rValueStr = rValueAt(
            stop = stop,
            pnl = pnl,
        ).toString()

        return " | ${rValueStr}R"
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
        val isProfitable: Boolean,
        val generated: Flow<Generated>,
    )

    private data class Generated(
        val stop: String,
        val target: String,
        val pnl: String,
        val inTrade: AnnotatedString,
        val inSession: AnnotatedString,
    )

    private object Schema : TableSchema() {

        val id = cell(Fixed(48.dp))
        val symbol = cell(Fixed(150.dp))
        val side = cell(Fixed(100.dp))
        val quantity = cell(Fixed(100.dp))
        val avgEntry = cell(Fixed(100.dp))
        val avgExit = cell(Fixed(100.dp))
        val entryTime = cell(Fixed(200.dp))
        val duration = cell(Fixed(100.dp))
        val stop = cell(Fixed(100.dp))
        val target = cell(Fixed(100.dp))
        val pnl = cell(Fixed(200.dp))
        val excursionsInTrade = cell()
        val excursionsInSession = cell()
    }

    @ContributesIntoSet(StatsGraph::class, binding<Study.Factory<out Study>>())
    @Inject
    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<PNLExcursionStudy> {

        override val name: String = "PNL Excursion"

        override fun create() = PNLExcursionStudy(profileId, tradingProfiles)
    }
}

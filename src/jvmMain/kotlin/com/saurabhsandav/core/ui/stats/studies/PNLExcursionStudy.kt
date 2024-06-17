package com.saurabhsandav.core.ui.stats.studies

import androidx.compose.foundation.TooltipArea
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
import com.saurabhsandav.core.thirdparty.paging_compose.collectAsLazyPagingItems
import com.saurabhsandav.core.thirdparty.paging_compose.itemKey
import com.saurabhsandav.core.trades.*
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeFilter
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeSort
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.Tooltip
import com.saurabhsandav.core.ui.common.TradeDateTimeFormat
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Fixed
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

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
                    excursionsInTrade {

                        TooltipArea(
                            tooltip = { Tooltip("Excursions In Trade") },
                            content = { Text("In Trade") },
                        )
                    }
                    excursionsInSession {

                        TooltipArea(
                            tooltip = { Tooltip("Excursions In Session") },
                            content = { Text("In Session") },
                        )
                    }
                }
            },
        ) {

            items(
                count = items.itemCount,
                key = items.itemKey { it.id }
            ) { index ->

                val item = items[index]!!
                val generated = item.generated.collectAsState(null).value

                Schema.SimpleRow {
                    id.text { item.id.value.toString() }
                    ticker.text { item.ticker }
                    side {
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
                        pnl {
                            Text(
                                generated.pnl,
                                color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                            )
                        }
                        excursionsInTrade { Text(generated.inTrade) }
                        excursionsInSession { Text(generated.inSession) }
                    }
                }

                HorizontalDivider()
            }
        }
    }

    private val data: Flow<PagingData<Item>> = flow {

        val pagingConfig = PagingConfig(
            pageSize = 70,
            enablePlaceholders = false,
            maxSize = 300,
        )

        val trades = tradingProfiles.getRecord(profileId).trades

        Pager(
            config = pagingConfig,
            pagingSourceFactory = {

                trades.getFilteredPagingSource(
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
                    isProfitable = trade.pnl > BigDecimal.ZERO,
                    generated = combine(
                        trades.getPrimaryStop(trade.id),
                        trades.getPrimaryTarget(trade.id),
                        trades.getExcursions(trade.id),
                    ) { stop, target, excursions ->

                        val rValue = stop?.let { trade.rValueAt(pnl = trade.pnl, stop = it) }
                        val rValueStr = rValue?.let { " | ${it.toPlainString()}R" }.orEmpty()

                        Generated(
                            stop = stop?.price?.toPlainString() ?: "NA",
                            target = target?.price?.toPlainString() ?: "NA",
                            pnl = "${trade.pnl.toPlainString()}${rValueStr}",
                            inTrade = trade.buildExcursionString(stop, excursions, true),
                            inSession = trade.buildExcursionString(stop, excursions, false),
                        )
                    },
                )
            }

        }.emitInto(this)
    }

    private fun Trade.buildExcursionString(
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

    private fun Trade.getRString(
        pnl: BigDecimal,
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
        val ticker = cell(Fixed(150.dp))
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

    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<PNLExcursionStudy> {

        override val name: String = "PNL Excursion"

        override fun create() = PNLExcursionStudy(profileId, tradingProfiles)
    }
}

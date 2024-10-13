package com.saurabhsandav.core.ui.stats.studies

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.brokerageAtExit
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.rValueAt
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.utils.emitInto
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

internal class PNLByDayStudy(
    profileId: ProfileId,
    tradingProfiles: TradingProfiles,
) : Study {

    @Composable
    override fun render() {

        val items by data.collectAsState(emptyList())

        LazyTable(
            modifier = Modifier.fillMaxSize(),
            headerContent = {

                Schema.SimpleHeader {
                    day.text { "Day" }
                    trades.text { "Trades" }
                    pnl.text { "PNL" }
                    netPnl.text { "Net PNL" }
                    fees.text { "Fees" }
                    rValue.text { "R" }
                }
            },
        ) {

            items(
                items = items,
            ) { item ->

                Column {

                    Schema.SimpleRow {
                        day.text { item.day }
                        trades.text { item.noOfTrades }
                        pnl {
                            Text(
                                text = item.pnl,
                                color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                            )
                        }
                        netPnl {
                            Text(
                                text = item.netPnl,
                                color = if (item.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                            )
                        }
                        fees.text { item.fees }
                        rValue {
                            Text(
                                text = item.rValue,
                                color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                            )
                        }
                    }

                    HorizontalDivider()
                }
            }
        }
    }

    private val data: Flow<List<Model>> = flow {

        val tradingRecord = tradingProfiles.getRecord(profileId)

        tradingRecord.trades.allTrades.flatMapLatest { allTrades ->

            allTrades
                .groupBy { it.entryTimestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date }
                .map { (date, tradesByDay) ->

                    val closedTrades = tradesByDay.filter { it.isClosed }
                    val closedTradesIds = closedTrades.map { it.id }

                    tradingRecord.stops.getPrimaryStops(closedTradesIds).map { stops ->

                        val dailyStats = closedTrades.map { trade ->

                            val brokerage = trade.brokerageAtExit()!!
                            val pnlBD = brokerage.pnl
                            val netPnlBD = brokerage.netPNL

                            val stop = stops.find { it.tradeId == trade.id }
                            val rValue = stop?.let { trade.rValueAt(pnl = pnlBD, stop = it) }

                            Triple(pnlBD, netPnlBD, rValue)
                        }

                        val pnl = dailyStats.sumOf { it.first }
                        val netPnl = dailyStats.sumOf { it.second }
                        val rValue = dailyStats.mapNotNull { it.third }.sumOf { it }

                        val day = date.format(DateFormat)

                        Model(
                            day = day,
                            noOfTrades = tradesByDay.size.toString(),
                            pnl = pnl.toPlainString(),
                            isProfitable = pnl > BigDecimal.ZERO,
                            netPnl = netPnl.toPlainString(),
                            isNetProfitable = netPnl > BigDecimal.ZERO,
                            fees = (pnl - netPnl).toPlainString(),
                            rValue = "${rValue}R",
                        )
                    }
                }.let { flows -> combine(flows) { it.asList() } }
        }.emitInto(this)
    }

    data class Model(
        val day: String,
        val noOfTrades: String,
        val pnl: String,
        val isProfitable: Boolean,
        val netPnl: String,
        val isNetProfitable: Boolean,
        val fees: String,
        val rValue: String,
    )

    private object Schema : TableSchema() {

        val day = cell()
        val trades = cell()
        val pnl = cell()
        val netPnl = cell()
        val fees = cell()
        val rValue = cell()
    }

    class Factory(
        private val profileId: ProfileId,
        private val tradingProfiles: TradingProfiles,
    ) : Study.Factory<PNLByDayStudy> {

        override val name: String = "PNL By Day"

        override fun create() = PNLByDayStudy(profileId, tradingProfiles)
    }

    private companion object {

        val DateFormat: DateTimeFormat<LocalDate> = LocalDate.Format {
            dayOfMonth(Padding.NONE)
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
        }
    }
}

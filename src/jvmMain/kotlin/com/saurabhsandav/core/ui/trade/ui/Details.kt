package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.table2.SimpleHeader
import com.saurabhsandav.core.ui.common.table2.SimpleRow
import com.saurabhsandav.core.ui.common.table2.TableCell.Width.Weight
import com.saurabhsandav.core.ui.common.table2.TableSchema
import com.saurabhsandav.core.ui.common.table2.text
import com.saurabhsandav.core.ui.trade.model.TradeState.Details

@Composable
internal fun Details(details: Details) {

    Column(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {

            DetailsTableSchema.SimpleHeader {
                broker.text { "Broker" }
                ticker.text { "Ticker" }
                side.text { "Side" }
                quantity.text { "Quantity" }
                avgEntry.text { "Avg. Entry" }
                avgExit.text { "Avg. Exit" }
                duration.text { "Duration" }
                pnl.text { "PNL" }
                netPnl.text { "Net PNL" }
                fees.text { "Fees" }
            }

            HorizontalDivider()

            DetailsTableSchema.SimpleRow {
                broker.text { details.broker }
                ticker.text { details.ticker }
                side {

                    Text(
                        text = details.side,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (details.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed,
                    )
                }
                quantity.text { details.quantity }
                avgEntry.text { details.entry }
                avgExit.text { details.exit ?: "NA" }
                duration {

                    Text(
                        text = details.duration.collectAsState("").value,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                pnl {

                    Text(
                        text = details.pnl,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (details.isProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                    )
                }
                netPnl {

                    Text(
                        text = details.netPnl,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (details.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed,
                    )
                }
                fees.text { details.fees }
            }
        }
    }
}

private object DetailsTableSchema : TableSchema() {

    val broker = cell(Weight(2F))
    val ticker = cell(Weight(1.7F))
    val side = cell()
    val quantity = cell()
    val avgEntry = cell()
    val avgExit = cell()
    val duration = cell(Weight(1.5F))
    val pnl = cell()
    val netPnl = cell()
    val fees = cell()
}

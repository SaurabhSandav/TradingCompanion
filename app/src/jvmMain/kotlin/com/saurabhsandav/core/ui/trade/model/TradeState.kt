package com.saurabhsandav.core.ui.trade.model

import com.saurabhsandav.core.trades.model.AttachmentFileId
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeNoteId
import com.saurabhsandav.core.ui.tags.model.TradeTag
import com.saurabhsandav.core.ui.trade.StopPreviewer
import com.saurabhsandav.core.ui.trade.TargetPreviewer
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

internal data class TradeState(
    val title: String,
    val details: Details?,
    val executions: List<Execution>,
    val newExecutionEnabled: Boolean,
    val stops: List<TradeStop>,
    val stopPreviewer: Flow<StopPreviewer>,
    val targets: List<TradeTarget>,
    val showTargetRValues: Boolean,
    val targetPreviewer: Flow<TargetPreviewer>,
    val excursions: Excursions?,
    val notes: List<TradeNote>,
    val tags: List<TradeTag>,
    val attachments: List<TradeAttachment>,
    val eventSink: (TradeEvent) -> Unit,
) {

    internal data class Details(
        val id: TradeId,
        val broker: String,
        val ticker: String,
        val side: String,
        val quantity: String,
        val entry: String,
        val exit: String?,
        val duration: Duration,
        val pnl: String?,
        val isProfitable: Boolean,
        val netPnl: String?,
        val isNetProfitable: Boolean,
        val fees: String?,
    ) {

        sealed class Duration {

            data class Open(val flow: Flow<String>) : Duration()

            data class Closed(val str: String) : Duration()
        }
    }

    internal data class Execution(
        val id: TradeExecutionId,
        val quantity: String,
        val side: String,
        val price: String,
        val timestamp: String,
        val locked: Boolean,
    )

    internal data class Excursions(
        val maeInTrade: String,
        val mfeInTrade: String,
        val maeInSession: String,
        val mfeInSession: String,
    )

    internal data class TradeStop(
        val price: BigDecimal,
        val priceText: String,
        val risk: String,
        val netRisk: String,
        val isPrimary: Boolean,
    )

    internal data class TradeTarget(
        val price: BigDecimal,
        val priceText: String,
        val rValue: String,
        val profit: String,
        val netProfit: String,
        val isPrimary: Boolean,
    )

    internal data class TradeAttachment(
        val fileId: AttachmentFileId,
        val name: String,
        val description: String?,
        val path: String,
        val extension: String?,
    )

    internal data class TradeNote(
        val id: TradeNoteId,
        val noteText: String,
        val dateText: String,
        val isMarkdown: Boolean,
    )
}

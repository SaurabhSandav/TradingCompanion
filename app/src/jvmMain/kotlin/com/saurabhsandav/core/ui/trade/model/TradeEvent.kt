package com.saurabhsandav.core.ui.trade.model

import com.saurabhsandav.core.trades.model.AttachmentFileId
import com.saurabhsandav.core.trades.model.TradeExecutionId
import com.saurabhsandav.core.trades.model.TradeNoteId
import com.saurabhsandav.core.trades.model.TradeTagId
import java.math.BigDecimal

internal sealed class TradeEvent {

    data object AddToTrade : TradeEvent()

    data object CloseTrade : TradeEvent()

    data class NewFromExistingExecution(val fromExecutionId: TradeExecutionId) : TradeEvent()

    data class EditExecution(val executionId: TradeExecutionId) : TradeEvent()

    data class LockExecution(val executionId: TradeExecutionId) : TradeEvent()

    data class DeleteExecution(val executionId: TradeExecutionId) : TradeEvent()

    data object OpenChart : TradeEvent()

    data class AddStop(val price: BigDecimal) : TradeEvent()

    data class DeleteStop(val price: BigDecimal) : TradeEvent()

    data class SetPrimaryStop(val price: BigDecimal) : TradeEvent()

    data class AddTarget(val price: BigDecimal) : TradeEvent()

    data class DeleteTarget(val price: BigDecimal) : TradeEvent()

    data class SetPrimaryTarget(val price: BigDecimal) : TradeEvent()

    data class AddTag(val id: TradeTagId) : TradeEvent()

    data class RemoveTag(val id: TradeTagId) : TradeEvent()

    data class AddAttachment(val formModel: AttachmentFormModel) : TradeEvent()

    data class UpdateAttachment(
        val fileId: AttachmentFileId,
        val formModel: AttachmentFormModel,
    ) : TradeEvent()

    data class RemoveAttachment(val fileId: AttachmentFileId) : TradeEvent()

    data class AddNote(
        val note: String,
        val isMarkdown: Boolean,
    ) : TradeEvent()

    data class UpdateNote(
        val id: TradeNoteId,
        val note: String,
        val isMarkdown: Boolean,
    ) : TradeEvent()

    data class DeleteNote(val id: TradeNoteId) : TradeEvent()
}

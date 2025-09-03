package com.saurabhsandav.core.ui.trade.model

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.record.model.AttachmentFileId
import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.model.TradeNoteId
import com.saurabhsandav.trading.record.model.TradeTagId

internal sealed class TradeEvent {

    data object AddToTrade : TradeEvent()

    data object CloseTrade : TradeEvent()

    data class NewFromExistingExecution(
        val fromExecutionId: TradeExecutionId,
    ) : TradeEvent()

    data class EditExecution(
        val executionId: TradeExecutionId,
    ) : TradeEvent()

    data class LockExecution(
        val executionId: TradeExecutionId,
    ) : TradeEvent()

    data class DeleteExecution(
        val executionId: TradeExecutionId,
    ) : TradeEvent()

    data object OpenChart : TradeEvent()

    data class AddStop(
        val price: KBigDecimal,
    ) : TradeEvent()

    data class DeleteStop(
        val price: KBigDecimal,
    ) : TradeEvent()

    data class SetPrimaryStop(
        val price: KBigDecimal,
    ) : TradeEvent()

    data class AddTarget(
        val price: KBigDecimal,
    ) : TradeEvent()

    data class DeleteTarget(
        val price: KBigDecimal,
    ) : TradeEvent()

    data class SetPrimaryTarget(
        val price: KBigDecimal,
    ) : TradeEvent()

    data class AddTag(
        val id: TradeTagId,
    ) : TradeEvent()

    data class RemoveTag(
        val id: TradeTagId,
    ) : TradeEvent()

    data class RemoveAttachment(
        val fileId: AttachmentFileId,
    ) : TradeEvent()

    data class AddNote(
        val note: String,
    ) : TradeEvent()

    data class UpdateNote(
        val id: TradeNoteId,
        val note: String,
    ) : TradeEvent()

    data class DeleteNote(
        val id: TradeNoteId,
    ) : TradeEvent()
}

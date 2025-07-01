package com.saurabhsandav.core.ui.attachmentform.model

import com.saurabhsandav.trading.record.model.AttachmentFileId
import com.saurabhsandav.trading.record.model.TradeId

internal sealed class AttachmentFormType {

    data class New(
        val tradeIds: List<TradeId>,
        val file: String? = null,
        val showPickerOnOpen: Boolean = file == null,
    ) : AttachmentFormType()

    data class Edit(
        val tradeId: TradeId,
        val fileId: AttachmentFileId,
    ) : AttachmentFormType()
}

package com.saurabhsandav.core.trading.record.model

import java.nio.file.Path

data class AttachmentWithFile(
    val tradeId: TradeId,
    val fileId: AttachmentFileId,
    val name: String,
    val description: String,
    val mimeType: String?,
    val path: Path,
    val checksum: String,
)

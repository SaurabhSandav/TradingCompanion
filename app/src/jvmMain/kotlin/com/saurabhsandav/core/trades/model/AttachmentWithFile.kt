package com.saurabhsandav.core.trades.model

import java.nio.file.Path

data class AttachmentWithFile(
    val tradeId: TradeId,
    val fileId: AttachmentFileId,
    val name: String,
    val description: String,
    val path: Path,
    val checksum: String,
)

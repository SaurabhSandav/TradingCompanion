package com.saurabhsandav.trading.record.model

import app.cash.sqldelight.ColumnAdapter

object TradeIdColumnAdapter : ColumnAdapter<TradeId, Long> {
    override fun decode(databaseValue: Long): TradeId = TradeId(databaseValue)

    override fun encode(value: TradeId): Long = value.value
}

object TradeExecutionIdColumnAdapter : ColumnAdapter<TradeExecutionId, Long> {
    override fun decode(databaseValue: Long): TradeExecutionId = TradeExecutionId(databaseValue)

    override fun encode(value: TradeExecutionId): Long = value.value
}

object SizingTradeIdColumnAdapter : ColumnAdapter<SizingTradeId, Long> {

    override fun decode(databaseValue: Long): SizingTradeId = SizingTradeId(databaseValue)

    override fun encode(value: SizingTradeId): Long = value.value
}

object AttachmentFileIdColumnAdapter : ColumnAdapter<AttachmentFileId, Long> {

    override fun decode(databaseValue: Long): AttachmentFileId = AttachmentFileId(databaseValue)

    override fun encode(value: AttachmentFileId): Long = value.value
}

object TradeNoteIdColumnAdapter : ColumnAdapter<TradeNoteId, Long> {

    override fun decode(databaseValue: Long): TradeNoteId = TradeNoteId(databaseValue)

    override fun encode(value: TradeNoteId): Long = value.value
}

object TradeTagIdColumnAdapter : ColumnAdapter<TradeTagId, Long> {

    override fun decode(databaseValue: Long): TradeTagId = TradeTagId(databaseValue)

    override fun encode(value: TradeTagId): Long = value.value
}

object ReviewIdColumnAdapter : ColumnAdapter<ReviewId, Long> {

    override fun decode(databaseValue: Long): ReviewId = ReviewId(databaseValue)

    override fun encode(value: ReviewId): Long = value.value
}

object TradeIdListColumnAdapter : ColumnAdapter<List<TradeId>, String> {

    override fun decode(databaseValue: String): List<TradeId> {
        return databaseValue.removeSurrounding(prefix = "[", suffix = "]")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { TradeId(it.toLong()) }
    }

    override fun encode(value: List<TradeId>): String = value.map { it.value }.joinToString()
}

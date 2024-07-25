package com.saurabhsandav.core.trades.model

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ProfileIdColumnAdapter : ColumnAdapter<ProfileId, Long> {
    override fun decode(databaseValue: Long): ProfileId = ProfileId(databaseValue)
    override fun encode(value: ProfileId): Long = value.value
}

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

object TradeAttachmentIdColumnAdapter : ColumnAdapter<TradeAttachmentId, Long> {

    override fun decode(databaseValue: Long): TradeAttachmentId = TradeAttachmentId(databaseValue)
    override fun encode(value: TradeAttachmentId): Long = value.value
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

    override fun decode(databaseValue: String): List<TradeId> = Json.decodeFromString(databaseValue)
    override fun encode(value: List<TradeId>): String = Json.encodeToString(value)
}

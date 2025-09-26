package com.saurabhsandav.trading.record.model

import kotlinx.serialization.Serializable

@JvmInline
value class SizingTradeId(
    val value: Long,
) {

    override fun toString(): String = value.toString()

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<SizingTradeId, Long> {

        override fun decode(databaseValue: Long): SizingTradeId = SizingTradeId(databaseValue)

        override fun encode(value: SizingTradeId): Long = value.value
    }
}

@JvmInline
@Serializable
value class TradeId(
    val value: Long,
) {

    override fun toString(): String = value.toString()

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<TradeId, Long> {
        override fun decode(databaseValue: Long): TradeId = TradeId(databaseValue)

        override fun encode(value: TradeId): Long = value.value
    }
}

@JvmInline
value class TradeExecutionId(
    val value: Long,
) {

    override fun toString(): String = value.toString()

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<TradeExecutionId, Long> {
        override fun decode(databaseValue: Long): TradeExecutionId = TradeExecutionId(databaseValue)

        override fun encode(value: TradeExecutionId): Long = value.value
    }
}

@JvmInline
value class AttachmentFileId(
    val value: Long,
) {

    override fun toString(): String = value.toString()

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<AttachmentFileId, Long> {

        override fun decode(databaseValue: Long): AttachmentFileId = AttachmentFileId(databaseValue)

        override fun encode(value: AttachmentFileId): Long = value.value
    }
}

@JvmInline
value class TradeNoteId(
    val value: Long,
) {

    override fun toString(): String = value.toString()

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<TradeNoteId, Long> {

        override fun decode(databaseValue: Long): TradeNoteId = TradeNoteId(databaseValue)

        override fun encode(value: TradeNoteId): Long = value.value
    }
}

@JvmInline
value class TradeTagId(
    val value: Long,
) {

    override fun toString(): String = value.toString()

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<TradeTagId, Long> {

        override fun decode(databaseValue: Long): TradeTagId = TradeTagId(databaseValue)

        override fun encode(value: TradeTagId): Long = value.value
    }
}

@JvmInline
value class ReviewId(
    val value: Long,
) {

    override fun toString(): String = value.toString()

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<ReviewId, Long> {

        override fun decode(databaseValue: Long): ReviewId = ReviewId(databaseValue)

        override fun encode(value: ReviewId): Long = value.value
    }
}

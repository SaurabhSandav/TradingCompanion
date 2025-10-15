package com.saurabhsandav.trading.record.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class SizingTradeId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class TradeId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class TradeExecutionId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class AttachmentFileId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class TradeNoteId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class TradeTagId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class ReviewId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}

package com.saurabhsandav.core.trades.model

import kotlinx.serialization.Serializable

@JvmInline
value class ProfileId(val value: Long) {

    override fun toString(): String = value.toString()
}

@JvmInline
value class SizingTradeId(val value: Long) {

    override fun toString(): String = value.toString()
}

@JvmInline
@Serializable
value class TradeId(val value: Long) {

    override fun toString(): String = value.toString()
}

@JvmInline
value class TradeExecutionId(val value: Long) {

    override fun toString(): String = value.toString()
}

@JvmInline
value class TradeAttachmentId(val value: Long) {

    override fun toString(): String = value.toString()
}

@JvmInline
value class TradeNoteId(val value: Long) {

    override fun toString(): String = value.toString()
}

@JvmInline
value class TradeTagId(val value: Long) {

    override fun toString(): String = value.toString()
}

@JvmInline
value class ReviewId(val value: Long) {

    override fun toString(): String = value.toString()
}

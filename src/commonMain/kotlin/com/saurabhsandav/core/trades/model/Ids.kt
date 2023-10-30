package com.saurabhsandav.core.trades.model

@JvmInline
value class ProfileId(val value: Long) {

    override fun toString(): String = value.toString()
}

@JvmInline
value class SizingTradeId(val value: Long) {

    override fun toString(): String = value.toString()
}

@JvmInline
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

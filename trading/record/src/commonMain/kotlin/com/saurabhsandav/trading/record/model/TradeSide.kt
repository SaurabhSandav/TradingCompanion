package com.saurabhsandav.trading.record.model

enum class TradeSide(
    val strValue: String,
) {
    Long("long"),
    Short("short"),
    ;

    companion object {

        fun fromString(strValue: String) = when (strValue.lowercase()) {
            "long" -> Long
            "short" -> Short
            else -> error("Invalid side")
        }
    }

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<TradeSide, String> {
        override fun decode(databaseValue: String): TradeSide = TradeSide.fromString(databaseValue)

        override fun encode(value: TradeSide): String = value.strValue
    }
}

val TradeSide.isLong: Boolean
    get() = this == TradeSide.Long

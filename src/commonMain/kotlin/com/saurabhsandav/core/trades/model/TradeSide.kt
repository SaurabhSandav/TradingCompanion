package com.saurabhsandav.core.trades.model

enum class TradeSide(val strValue: String) {
    Long("long"),
    Short("short");

    companion object {

        fun fromString(strValue: String) = when (strValue.lowercase()) {
            "long" -> Long
            "short" -> Short
            else -> error("Invalid side")
        }
    }

    object ColumnAdapter : com.squareup.sqldelight.ColumnAdapter<TradeSide, String> {
        override fun decode(databaseValue: String): TradeSide = TradeSide.fromString(databaseValue)

        override fun encode(value: TradeSide): String = value.strValue
    }
}

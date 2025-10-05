package com.saurabhsandav.trading.core

enum class Instrument(
    val strValue: String,
) {
    Index("index"),
    Equity("equity"),
    Futures("futures"),
    Options("options"),
    ;

    companion object {

        fun fromString(strValue: String) = when (strValue.lowercase()) {
            "index" -> Index
            "equity" -> Equity
            "futures" -> Futures
            "options" -> Options
            else -> error("Invalid instrument")
        }
    }
}

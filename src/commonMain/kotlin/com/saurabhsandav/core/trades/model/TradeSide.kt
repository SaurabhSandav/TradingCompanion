package com.saurabhsandav.core.trades.model

internal enum class TradeSide(val strValue: String) {
    Long("long"),
    Short("short");

    companion object {

        fun fromString(strValue: String) = when (strValue.lowercase()) {
            "long" -> Long
            "short" -> Short
            else -> error("Invalid side")
        }
    }
}

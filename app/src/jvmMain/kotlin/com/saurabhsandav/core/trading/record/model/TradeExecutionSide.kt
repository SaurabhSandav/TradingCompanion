package com.saurabhsandav.core.trading.record.model

actual enum class TradeExecutionSide(
    val strValue: String,
) {
    Buy("buy"),
    Sell("sell"),
    ;

    companion object {

        fun fromString(strValue: String) = when (strValue.lowercase()) {
            "buy" -> Buy
            "sell" -> Sell
            else -> error("Invalid trade execution side: $strValue")
        }
    }

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<TradeExecutionSide, String> {
        override fun decode(databaseValue: String): TradeExecutionSide = TradeExecutionSide.fromString(databaseValue)

        override fun encode(value: TradeExecutionSide): String = value.strValue
    }
}

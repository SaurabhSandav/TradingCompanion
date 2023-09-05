package com.saurabhsandav.core.trades.model

actual enum class OrderSide(val strValue: String) {
    Buy("buy"),
    Sell("sell");

    companion object {

        fun fromString(strValue: String) = when (strValue.lowercase()) {
            "buy" -> Buy
            "sell" -> Sell
            else -> error("Invalid order side: $strValue")
        }
    }

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<OrderSide, String> {
        override fun decode(databaseValue: String): OrderSide = OrderSide.fromString(databaseValue)

        override fun encode(value: OrderSide): String = value.strValue
    }
}

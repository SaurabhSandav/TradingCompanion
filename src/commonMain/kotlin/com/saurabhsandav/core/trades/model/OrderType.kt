package com.saurabhsandav.core.trades.model

enum class OrderType(val strValue: String) {
    Buy("buy"),
    Sell("sell");

    companion object {

        fun fromString(strValue: String) = when (strValue.lowercase()) {
            "buy" -> Buy
            "sell" -> Sell
            else -> error("Invalid type")
        }
    }

    object OrderTypeColumnAdapter : com.squareup.sqldelight.ColumnAdapter<OrderType, String> {
        override fun decode(databaseValue: String): OrderType = OrderType.fromString(databaseValue)

        override fun encode(value: OrderType): String = value.strValue
    }
}

package trades.model

internal enum class OrderType(val strValue: String) {
    Buy("buy"),
    Sell("sell");

    companion object {

        fun fromString(strValue: String) = when (strValue.lowercase()) {
            "buy" -> Buy
            "sell" -> Sell
            else -> error("Invalid type")
        }
    }
}

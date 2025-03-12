package com.saurabhsandav.core.trades.model

actual enum class Instrument(
    val strValue: String,
) {
    Equity("equity"),
    Futures("futures"),
    Options("options"),
    ;

    companion object {

        fun fromString(strValue: String) = when (strValue.lowercase()) {
            "equity" -> Equity
            "futures" -> Futures
            "options" -> Options
            else -> error("Invalid instrument")
        }
    }

    object ColumnAdapter : app.cash.sqldelight.ColumnAdapter<Instrument, String> {
        override fun decode(databaseValue: String): Instrument = Instrument.fromString(databaseValue)

        override fun encode(value: Instrument): String = value.strValue
    }
}
